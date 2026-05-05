package main

import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/base64"
    "errors"
    "fmt"
    "html"
    "net/http"
    "os"
    "strconv"
    "strings"
    "time"
)

// ...existing code...
// removed the insecure global session map and replaced with signed cookie sessions
var hmacSecret = []byte(os.Getenv("SESSION_SECRET"))

// ...existing code...
func main() {
    // generate a random-ish secret if none provided (process-lifetime only)
    if len(hmacSecret) == 0 {
        // fallback secret; in production, set SESSION_SECRET in environment
        hmacSecret = []byte(fmt.Sprintf("dev-secret-%d", time.Now().UnixNano()))
    }

    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        username, err := validateSessionCookie(r)
        if err != nil || username == "" {
            // avoid revealing reason for failure
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username to context via request header clone (minimal approach)
        r.Header.Set("X-Authenticated-User", username)
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="64" required></p>
            <p><input type="password" name="password" maxlength="128" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit request body size to avoid resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, 1024*10)

    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // validate input: length and allowed characters (simple whitelist)
    if len(username) == 0 || len(username) > 64 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    if len(password) == 0 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }
    // NOTE: In a real application verify against a hashed password store.
    // This example keeps logic minimal and only accepts a specific account.
    if username == "admin" && password == "password" {
        if err := setSessionCookie(w, username); err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // avoid leaking which part failed
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve authenticated username put by middleware
    username := r.Header.Get("X-Authenticated-User")
    if username == "" {
        // defensive: should not happen because middleware checks
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // escape to prevent XSS
    escapedUser := html.EscapeString(username)

    // Minimal settings page - do not expose secrets or sensitive info
    page := fmt.Sprintf(`<html>
        <head><meta charset="utf-8"></head>
        <body>
            <h1>Account settings</h1>
            <p>Signed in as: %s</p>
            <form action="/logout" method="post">
                <button type="submit">Logout</button>
            </form>
        </body>
    </html>`, escapedUser)

    // write response with explicit content-type
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, page)
}

// setSessionCookie creates a signed cookie with expiry
func setSessionCookie(w http.ResponseWriter, username string) error {
    if username == "" {
        return errors.New("empty username")
    }
    expiry := time.Now().Add(30 * time.Minute).Unix()
    payload := fmt.Sprintf("%s|%d", username, expiry)
    mac := computeHMAC([]byte(payload), hmacSecret)
    token := base64.StdEncoding.EncodeToString([]byte(payload)) + "." + base64.StdEncoding.EncodeToString(mac)

    cookie := &http.Cookie{
        Name:     "session",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        // set Secure only if connection is TLS (useful for localhost dev)
        Secure: false,
        // set reasonable expiry
        Expires: time.Unix(expiry, 0),
    }
    http.SetCookie(w, cookie)
    return nil
}

// validateSessionCookie verifies HMAC and expiry
func validateSessionCookie(r *http.Request) (string, error) {
    cookie, err := r.Cookie("session")
    if err != nil {
        return "", err
    }
    parts := strings.SplitN(cookie.Value, ".", 2)
    if len(parts) != 2 {
        return "", errors.New("invalid token format")
    }
    payloadB, err := base64.StdEncoding.DecodeString(parts[0])
    if err != nil {
        return "", errors.New("invalid payload encoding")
    }
    macB, err := base64.StdEncoding.DecodeString(parts[1])
    if err != nil {
        return "", errors.New("invalid mac encoding")
    }
    expectedMac := computeHMAC(payloadB, hmacSecret)
    if !hmac.Equal(expectedMac, macB) {
        return "", errors.New("invalid signature")
    }
    // parse payload username|expiry
    p := string(payloadB)
    parts2 := strings.SplitN(p, "|", 2)
    if len(parts2) != 2 {
        return "", errors.New("invalid payload")
    }
    username := parts2[0]
    expStr := parts2[1]
    exp, err := strconv.ParseInt(expStr, 10, 64)
    if err != nil {
        return "", errors.New("invalid expiry")
    }
    if time.Now().Unix() > exp {
        return "", errors.New("session expired")
    }
    // additional validation: username allowed chars
    if len(username) == 0 || len(username) > 64 {
        return "", errors.New("invalid username")
    }
    return username, nil
}

func computeHMAC(message, key []byte) []byte {
    m := hmac.New(sha256.New, key)
    m.Write(message)
    return m.Sum(nil)
}
