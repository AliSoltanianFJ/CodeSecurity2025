package main

import (
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "crypto/subtle"
    "encoding/base64"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "strconv"
    "strings"
    "time"
)

// ...existing code...

// secure random key used to sign session cookies (generated at startup)
var sessionKey = generateRandomKey(32)

func generateRandomKey(n int) []byte {
    b := make([]byte, n)
    _, err := rand.Read(b)
    if err != nil {
        // if random generation fails, panic early (this is startup only)
        panic("unable to generate session key")
    }
    return b
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with authentication wrapper
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        username, ok := validateSessionCookie(r)
        if !ok || username == "" {
            // not authenticated
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // add the validated username to the request context via header (local to this process)
        // (in production use context.WithValue and r = r.WithContext(ctx))
        r.Header.Set("X-Authenticated-User", username)
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    // use template/text escaping on any dynamic content (none here)
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // simple form; output is static so safe
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="64"></p>
            <p><input type="password" name="password" maxlength="128"></p>
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

    // Validate and sanitize inputs
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // basic validation to avoid overly long inputs
    if len(username) == 0 || len(username) > 64 || len(password) == 0 || len(password) > 128 {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Constant-time comparisons to reduce timing attack surface.
    const expectedUser = "admin"
    const expectedPass = "password" // in real apps, store hashed passwords (bcrypt/Argon2) and compare hashes

    // ensure same length before ConstantTimeCompare to avoid early zero result on length mismatch
    userOK := false
    if len(username) == len(expectedUser) {
        userOK = subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) == 1
    }
    passOK := false
    if len(password) == len(expectedPass) {
        passOK = subtle.ConstantTimeCompare([]byte(password), []byte(expectedPass)) == 1
    }

    if userOK && passOK {
        // create signed session cookie
        cookie := createSessionCookie(username)
        http.SetCookie(w, cookie)
        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    // generic error message to avoid revealing which part failed
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// createSessionCookie constructs a signed cookie containing the username and timestamp.
// The cookie value format: base64(payload) + "." + base64(signature)
// where payload = username|unix_ts
func createSessionCookie(username string) *http.Cookie {
    payload := username + "|" + strconv.FormatInt(time.Now().Unix(), 10)
    mac := hmac.New(sha256.New, sessionKey)
    mac.Write([]byte(payload))
    sig := mac.Sum(nil)

    encPayload := base64.RawURLEncoding.EncodeToString([]byte(payload))
    encSig := base64.RawURLEncoding.EncodeToString(sig)
    cookieVal := encPayload + "." + encSig

    // Note: Secure should be true in production (HTTPS). For local testing over HTTP keep false.
    return &http.Cookie{
        Name:     "session",
        Value:    cookieVal,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        // Secure:   true, // enable in production when serving over TLS
        Expires: time.Now().Add(24 * time.Hour),
    }
}

// validateSessionCookie verifies the HMAC and age of the session cookie. Returns username if valid.
func validateSessionCookie(r *http.Request) (string, bool) {
    c, err := r.Cookie("session")
    if err != nil {
        return "", false
    }
    parts := strings.SplitN(c.Value, ".", 2)
    if len(parts) != 2 {
        return "", false
    }
    encPayload, encSig := parts[0], parts[1]
    payloadBytes, err := base64.RawURLEncoding.DecodeString(encPayload)
    if err != nil {
        return "", false
    }
    sigBytes, err := base64.RawURLEncoding.DecodeString(encSig)
    if err != nil {
        return "", false
    }
    mac := hmac.New(sha256.New, sessionKey)
    mac.Write(payloadBytes)
    expectedSig := mac.Sum(nil)
    // constant time compare
    if len(sigBytes) != len(expectedSig) || subtle.ConstantTimeCompare(sigBytes, expectedSig) != 1 {
        return "", false
    }
    // payload is "username|ts"
    parts2 := strings.SplitN(string(payloadBytes), "|", 2)
    if len(parts2) != 2 {
        return "", false
    }
    username := parts2[0]
    ts, err := strconv.ParseInt(parts2[1], 10, 64)
    if err != nil {
        return "", false
    }
    // enforce expiration (24h)
    if time.Unix(ts, 0).Add(24 * time.Hour).Before(time.Now()) {
        return "", false
    }
    // additional validation: username allowed characters and length
    username = strings.TrimSpace(username)
    if username == "" || len(username) > 64 {
        return "", false
    }
    return username, true
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for settings view
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Retrieve authenticated username injected by ensureLoggedIn
    username := r.Header.Get("X-Authenticated-User")
    if username == "" {
        // defensive: validate cookie again if header not set
        u, ok := validateSessionCookie(r)
        if !ok {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        username = u
    }

    // Use html/template to automatically escape content and avoid XSS.
    const tpl = `
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Settings</title></head>
<body>
  <h1>Settings</h1>
  <p>Welcome, {{.User}}. Your settings are private.</p>
  <p><a href="/">Home</a></p>
</body>
</html>
`
    t, err := template.New("settings").Parse(tpl)
    if err != nil {
        // template parsing failure should not leak sensitive info
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    data := struct {
        User string
    }{
        User: username,
    }

    // Execute template (escaped) and write to ResponseWriter
    if err := t.Execute(w, data); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
}
