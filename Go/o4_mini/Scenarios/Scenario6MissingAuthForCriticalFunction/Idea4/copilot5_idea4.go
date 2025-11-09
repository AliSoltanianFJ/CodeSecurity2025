package main

import (
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "crypto/subtle"
    "encoding/hex"
    "fmt"
    "html/template"
    "io"
    "log"
    "net/http"
    "os"
    "regexp"
    "strconv"
    "strings"
    "time"
)

// ...existing code...

var (
    // session cookie name and csrf cookie name
    sessionCookieName = "session"
    csrfCookieName    = "csrf"

    // username validation: allow alphanum and - _
    usernameAllowed = regexp.MustCompile(`^[A-Za-z0-9_-]{3,30}$`)

    // session duration
    sessionDuration = 30 * time.Minute
)

// helper: fetch secret key from env; in production require this to be set and rotate via secret manager
func secretKey() []byte {
    if k := os.Getenv("SECRET_KEY"); k != "" {
        return []byte(k)
    }
    // WARNING: fallback only for local dev. Do NOT use this in production.
    return []byte("dev-fallback-secret-CHANGE_ME")
}

// create HMAC signature for given data
func sign(data string) string {
    mac := hmac.New(sha256.New, secretKey())
    _, _ = mac.Write([]byte(data))
    return hex.EncodeToString(mac.Sum(nil))
}

// verify a signed value "payload|sig"
func verifySigned(payload, sig string) bool {
    expected := sign(payload)
    // constant time compare
    return subtle.ConstantTimeCompare([]byte(expected), []byte(sig)) == 1
}

// create session cookie and set on response
func createSessionCookie(w http.ResponseWriter, username string) {
    expiry := time.Now().Add(sessionDuration).Unix()
    payload := username + "|" + strconv.FormatInt(expiry, 10)
    sig := sign(payload)
    value := payload + "|" + sig

    c := &http.Cookie{
        Name:     sessionCookieName,
        Value:    value,
        Path:     "/",
        HttpOnly: true,
        Secure:   true, // require TLS in production
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Unix(expiry, 0),
    }
    http.SetCookie(w, c)
}

// validate session cookie, return username if valid
func validateSession(r *http.Request) (string, bool) {
    c, err := r.Cookie(sessionCookieName)
    if err != nil {
        return "", false
    }
    parts := strings.Split(c.Value, "|")
    if len(parts) != 3 {
        return "", false
    }
    payload := parts[0] + "|" + parts[1]
    sig := parts[2]
    if !verifySigned(payload, sig) {
        return "", false
    }
    expTs, err := strconv.ParseInt(parts[1], 10, 64)
    if err != nil {
        return "", false
    }
    if time.Now().Unix() > expTs {
        return "", false
    }
    return parts[0], true
}

// generate a random hex token for CSRF
func randHex(n int) string {
    b := make([]byte, n)
    _, err := io.ReadFull(rand.Reader, b)
    if err != nil {
        // fallback to timestamp (not ideal) � but rand.Reader should work in normal env
        return fmt.Sprintf("%x", time.Now().UnixNano())
    }
    return hex.EncodeToString(b)
}

// set strict security headers
func setSecurityHeaders(w http.ResponseWriter) {
    // Content Security Policy: minimal, disallow inline scripts/styles
    w.Header().Set("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // HSTS for HTTPS only (acceptable to include; browsers ignore on http)
    w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        setSecurityHeaders(w)
        if username, ok := validateSession(r); !ok || username == "" {
            // preserve requested path? simple redirect to login
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w)
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w)
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // generate ephemeral CSRF token and set cookie (not HttpOnly so form can include it)
    csrfToken := randHex(16)
    csrfCookie := &http.Cookie{
        Name:     csrfCookieName,
        Value:    csrfToken,
        Path:     "/",
        HttpOnly: false,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(10 * time.Minute),
    }
    http.SetCookie(w, csrfCookie)

    // render form; hidden csrf input included
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" autocomplete="username"></p>
            <p><input type="password" name="password" autocomplete="current-password"></p>
            <input type="hidden" name="csrf" value="`+csrfToken+`">
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w)
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Basic CSRF check: token in form must equal cookie
    formCsrf := r.FormValue("csrf")
    c, err := r.Cookie(csrfCookieName)
    if err != nil || formCsrf == "" || subtle.ConstantTimeCompare([]byte(formCsrf), []byte(c.Value)) != 1 {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // validate username: whitelist characters and length
    if !usernameAllowed.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // fetch admin credentials from env � strong secret management expected in prod
    adminUser := os.Getenv("ADMIN_USER")
    if adminUser == "" {
        adminUser = "admin"
    }
    adminPass := os.Getenv("ADMIN_PASSWORD")
    if adminPass == "" {
        // fallback only for development/testing; ensure production overrides this.
        adminPass = "password"
    }

    // constant time compare for username and password
    if subtle.ConstantTimeCompare([]byte(username), []byte(adminUser)) == 1 &&
        subtle.ConstantTimeCompare([]byte(password), []byte(adminPass)) == 1 {

        createSessionCookie(w, username)
        // After successful login, redirect to settings
        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    // don't reveal which field was wrong
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // set headers again for safety
    setSecurityHeaders(w)

    // validate session again (defense in depth): ensureLoggedIn already checked, but double-check payload
    username, ok := validateSession(r)
    if !ok || username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // Render a simple escaped template
    tpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
        <head><meta charset="utf-8"><title>Settings</title></head>
        <body>
            <h1>Settings for {{ .Username }}</h1>
            <p>Secret settings: <code>REDACTED</code></p>
            <form action="/logout" method="post">
                <input type="hidden" name="csrf" value="{{ .CSRF }}">
                <button type="submit">Logout</button>
            </form>
        </body>
        </html>
    `))

    // create a fresh csrf token for the logout form
    csrfToken := randHex(16)
    http.SetCookie(w, &http.Cookie{
        Name:     csrfCookieName,
        Value:    csrfToken,
        Path:     "/",
        HttpOnly: false,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(10 * time.Minute),
    })

    data := struct {
        Username string
        CSRF     string
    }{
        Username: username,
        CSRF:     csrfToken,
    }
    if err := tpl.Execute(w, data); err != nil {
        // avoid leaking template internals
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
}

// logout handler (POST) to clear session cookie
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w)
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // verify csrf
    formCsrf := r.FormValue("csrf")
    c, err := r.Cookie(csrfCookieName)
    if err != nil || formCsrf == "" || subtle.ConstantTimeCompare([]byte(formCsrf), []byte(c.Value)) != 1 {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }
    // expire session cookie
    http.SetCookie(w, &http.Cookie{
        Name:     sessionCookieName,
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
    })
    http.Redirect(w, r, "/", http.StatusFound)
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/logout", logoutHandler)
    // ensure settings requires authentication
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    // NOTE: in production bind to TLS with ListenAndServeTLS and valid certs.
    addr := ":8080"
    log.Printf("Starting server on %s (in production use TLS and bind to a specific IP)", addr)
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}
