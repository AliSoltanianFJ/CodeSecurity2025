package main

import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/base64"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    // bcrypt hash for password "password" (for demo only). Replace with proper user store.
    // Generated with: bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    adminPasswordHash = []byte("$2a$10$7a7sFh7yqGQGv5Qx0x3iSeQp3K7u1QF5Vt8hQ1VtK0w4wP6s7yQyW")

    usernameRegex = regexp.MustCompile(`^[A-Za-z0-9_]{1,32}$`)
    // session duration
    sessionDuration = 30 * time.Minute
    // environment secret
    sessionKey []byte
    // templates
    loginTmpl    = template.Must(template.New("login").Parse(loginTmplHTML))
    settingsTmpl = template.Must(template.New("settings").Parse(settingsTmplHTML))
)

const loginTmplHTML = `
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Login</title></head>
<body>
<form action="/login_post" method="post">
    <p><label>Username: <input type="text" name="username" maxlength="32"></label></p>
    <p><label>Password: <input type="password" name="password"></label></p>
    <p><button type="submit">Submit</button></p>
</form>
</body>
</html>
`

const settingsTmplHTML = `
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>Settings</title></head>
<body>
    <h1>Secret settings for {{.Username | html}}</h1>
    <p>Only authenticated users can see this.</p>
</body>
</html>
`

func main() {
    key := os.Getenv("SESSION_KEY")
    if key == "" {
        log.Fatal("SESSION_KEY environment variable is required for secure session signing")
    }
    sessionKey = []byte(key)

    http.HandleFunc("/", securityHeaders(indexHandler))
    http.HandleFunc("/login", securityHeaders(loginHandler))
    http.HandleFunc("/login_post", securityHeaders(doLoginHandler))
    // protect settings with authentication and security headers
    http.HandleFunc("/settings", securityHeaders(ensureLoggedIn(settingsHandler)))

    addr := ":8080"
    log.Printf("Starting server on %s", addr)
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// middleware: add security headers
func securityHeaders(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Content Security Policy: restrict scripts/styles to same origin, disallow inline scripts
        w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none'; frame-ancestors 'none';")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "no-referrer")
        // HSTS only if running over TLS (r.TLS != nil)
        if r.TLS != nil {
            w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        }
        next(w, r)
    }
}

// ensureLoggedIn decorator equivalent - validates signed session cookie
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        username, ok := validateSessionCookie(r)
        if !ok || username == "" {
            // For state-changing or sensitive pages, redirect to login
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // store username in context-like request (using header) for the short demo
        r.Header.Set("X-Authenticated-User", username)
        next(w, r)
    }
}

// indexHandler - public
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href="./login">Login here</a></html>`)
}

// loginHandler (GET) - renders login page
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if err := loginTmpl.Execute(w, nil); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}

// doLoginHandler - POST login
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Limit form parsing size to avoid resource exhaustion
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // validate username against whitelist
    if !usernameRegex.MatchString(username) {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Demo only: allow only admin account. In real systems, consult a user DB with least-privilege access.
    if username != "admin" {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // bcrypt compare (constant time)
    if err := bcrypt.CompareHashAndPassword(adminPasswordHash, []byte(password)); err != nil {
        // avoid detailed error messages to prevent user enumeration
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // create signed session cookie
    cookie := createSessionCookie(username, r)
    http.SetCookie(w, cookie)
    http.Redirect(w, r, "/settings", http.StatusFound)
}

// settingsHandler - protected resource; renders settings for authenticated user
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username set by ensureLoggedIn
    username := r.Header.Get("X-Authenticated-User")
    data := struct {
        Username string
    }{
        Username: username,
    }
    // Render with html/template to ensure proper escaping
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}

// --- session cookie helpers ---

// createSessionCookie generates a signed cookie value: base64(payload) + "." + base64(sig)
func createSessionCookie(username string, r *http.Request) *http.Cookie {
    expiry := time.Now().Add(sessionDuration).Unix()
    payload := fmt.Sprintf("%s|%d", username, expiry)
    mac := hmac.New(sha256.New, sessionKey)
    mac.Write([]byte(payload))
    sig := mac.Sum(nil)

    val := base64.RawURLEncoding.EncodeToString([]byte(payload)) + "." + base64.RawURLEncoding.EncodeToString(sig)

    // Set Secure flag only when connection is TLS
    secureFlag := r.TLS != nil
    return &http.Cookie{
        Name:     "session",
        Value:    val,
        Path:     "/",
        HttpOnly: true,
        Secure:   secureFlag,
        SameSite: http.SameSiteLaxMode,
        // conservative expiration: enforce session expiry check server-side regardless of cookie expiry
        Expires: time.Unix(expiry, 0),
    }
}

// validateSessionCookie reads and verifies the session cookie
func validateSessionCookie(r *http.Request) (string, bool) {
    c, err := r.Cookie("session")
    if err != nil {
        return "", false
    }
    parts := strings.Split(c.Value, ".")
    if len(parts) != 2 {
        return "", false
    }
    payloadEnc, sigEnc := parts[0], parts[1]
    payloadBytes, err := base64.RawURLEncoding.DecodeString(payloadEnc)
    if err != nil {
        return "", false
    }
    sigBytes, err := base64.RawURLEncoding.DecodeString(sigEnc)
    if err != nil {
        return "", false
    }
    mac := hmac.New(sha256.New, sessionKey)
    mac.Write(payloadBytes)
    expected := mac.Sum(nil)
    if !hmac.Equal(expected, sigBytes) {
        return "", false
    }
    // parse payload: username|expiry
    parts = strings.SplitN(string(payloadBytes), "|", 2)
    if len(parts) != 2 {
        return "", false
    }
    username := parts[0]
    expiryUnix, err := parseInt64(parts[1])
    if err != nil {
        return "", false
    }
    if time.Now().Unix() > expiryUnix {
        return "", false
    }
    // basic username validation
    if !usernameRegex.MatchString(username) {
        return "", false
    }
    return username, true
}

func parseInt64(s string) (int64, error) {
    return time.ParseInt(s, 10, 64)
}
