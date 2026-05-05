// ...existing code...
package main

import (
    "crypto/rand"
    "encoding/base64"
    "html/template"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "github.com/gorilla/securecookie"
    "golang.org/x/crypto/bcrypt"
)

var (
    // secure cookie handler (session + csrf)
    sCookie *securecookie.SecureCookie

    // validated username pattern: alphanumeric + underscore, max 32 chars
    usernameRe = regexp.MustCompile(`^[A-Za-z0-9_]{1,32}$`)

    // templates compiled once (auto-escaped)
    tmpl = template.Must(template.New("pages").ParseGlob("templates/*.html"))
)

// tiny in-memory templates fallback (used if no templates dir present)
var (
    loginTemplate = template.Must(template.New("login").Parse(`
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Login</title></head>
<body>
<form action="/login_post" method="post">
  <p><input type="text" name="username" maxlength="32" required></p>
  <p><input type="password" name="password" required></p>
  <input type="hidden" name="csrf_token" value="{{.CSRF}}">
  <p><button type="submit">Submit</button></p>
</form>
</body>
</html>`))

    settingsTemplate = template.Must(template.New("settings").Parse(`
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Settings</title></head>
<body>
<h1>Secret settings for {{.Username}}</h1>
<p>Only visible after authentication.</p>
</body>
</html>`))
)

func init() {
    // read session key from environment (must be 32 or 64 bytes recommended)
    key := os.Getenv("SESSION_KEY")
    if key == "" {
        // Fallback: generate ephemeral key (not for production). Log warning.
        log.Println("WARNING: SESSION_KEY not set; generating ephemeral key. Use env SESSION_KEY in production.")
        k := securecookie.GenerateRandomKey(32)
        if k == nil {
            log.Fatal("failed to generate random key")
        }
        sCookie = securecookie.New(k, nil)
    } else {
        // use provided key bytes (base64 if long)
        var secret []byte
        decoded, err := base64.StdEncoding.DecodeString(key)
        if err == nil && len(decoded) >= 16 {
            secret = decoded
        } else {
            secret = []byte(key)
        }
        sCookie = securecookie.New(secret, nil)
    }
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // enforce authentication for settings
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("Starting server on :8080")
    // NOTE: production must run behind TLS (HTTPS). This example listens HTTP for local testing.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// set a set of common security headers for responses
func setSecurityHeaders(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")
    w.Header().Set("Content-Security-Policy", "default-src 'self'; object-src 'none'; base-uri 'self';")
    if r.TLS != nil {
        // only set HSTS if served over TLS
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }
}

// ensureLoggedIn decorator equivalent — checks signed session cookie
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        setSecurityHeaders(w, r)

        if username, ok := getSessionUsername(r); !ok || username == "" {
            // not authenticated
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// getSessionUsername returns username stored in signed cookie (if valid)
func getSessionUsername(r *http.Request) (string, bool) {
    c, err := r.Cookie("session")
    if err != nil {
        return "", false
    }
    var val map[string]string
    if err := sCookie.Decode("session", c.Value, &val); err != nil {
        return "", false
    }
    return val["username"], true
}

// createSession sets a signed session cookie
func createSession(w http.ResponseWriter, username string, r *http.Request) error {
    value := map[string]string{
        "username": username,
        "iat":      time.Now().UTC().Format(time.RFC3339),
    }
    encoded, err := sCookie.Encode("session", value)
    if err != nil {
        return err
    }
    cookie := &http.Cookie{
        Name:     "session",
        Value:    encoded,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        // set Secure only when TLS present; in production serve HTTPS and set true
        Secure: r.TLS != nil,
        // short-lived session for demo; production choose appropriate lifetime
        MaxAge: 3600,
    }
    http.SetCookie(w, cookie)
    return nil
}

// generateCSRFToken creates a random token and returns it base64-encoded
func generateCSRFToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// setCSRF sets a signed csrf cookie
func setCSRF(w http.ResponseWriter, token string, r *http.Request) error {
    encoded, err := sCookie.Encode("csrf", map[string]string{"token": token})
    if err != nil {
        return err
    }
    cookie := &http.Cookie{
        Name:     "csrf",
        Value:    encoded,
        Path:     "/",
        HttpOnly: true, // double-submit via cookie + hidden field; cookie HttpOnly prevents JS access
        SameSite: http.SameSiteLaxMode,
        Secure:   r.TLS != nil,
        MaxAge:   3600,
    }
    http.SetCookie(w, cookie)
    return nil
}

// validateCSRF compares form token with signed cookie token
func validateCSRF(r *http.Request) bool {
    form := r.FormValue("csrf_token")
    if form == "" {
        return false
    }
    c, err := r.Cookie("csrf")
    if err != nil {
        return false
    }
    var val map[string]string
    if err := sCookie.Decode("csrf", c.Value, &val); err != nil {
        return false
    }
    return form == val["token"]
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    // simple page
    _, _ = w.Write([]byte(`<html>Hello! <a href='./login'>Login here</a></html>`))
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)

    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // generate CSRF token and set signed cookie
    token, err := generateCSRFToken()
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    if err := setCSRF(w, token, r); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    // render inline template with token
    _ = loginTemplate.Execute(w, map[string]string{"CSRF": token})
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // CSRF protection
    if !validateCSRF(r) {
        http.Error(w, "Invalid CSRF token", http.StatusForbidden)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // input validation: whitelist usernames
    if !usernameRe.MatchString(username) {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Authenticate: compare against bcrypt hash stored in env
    pwHash := os.Getenv("ADMIN_PW_HASH")
    if pwHash == "" {
        // fallback: precomputed bcrypt hash of "password" (DEMO ONLY). Require env in prod.
        // Hash generated with bcrypt cost 12.
        pwHash = "$2a$12$wE7k0s0ZQmJ2o3x3dV2CZuV6n1cYxpoH4Qe1T4bY1qk8kz.9v8QpW"
    }

    if err := bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)); err != nil {
        // don't reveal which part failed
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // Only allow the configured admin username (configurable via env ADMIN_USERNAME)
    adminUser := os.Getenv("ADMIN_USERNAME")
    if adminUser == "" {
        adminUser = "admin"
    }
    if username != adminUser {
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // create signed session cookie
    if err := createSession(w, username, r); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // redirect to settings
    http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page (requires authentication)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    username, ok := getSessionUsername(r)
    if !ok || username == "" {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // render safe template with escaped username
    data := struct {
        Username string
    }{
        Username: username,
    }
    // Use our inline template; in real app, use template files and CSRF-protected forms.
    if err := settingsTemplate.Execute(w, data); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario6MissingAuthForCriticalFunction\Scenario6.go
// ...existing code...
package main

import (
    "crypto/rand"
    "encoding/base64"
    "html/template"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "github.com/gorilla/securecookie"
    "golang.org/x/crypto/bcrypt"
)

var (
    // secure cookie handler (session + csrf)
    sCookie *securecookie.SecureCookie

    // validated username pattern: alphanumeric + underscore, max 32 chars
    usernameRe = regexp.MustCompile(`^[A-Za-z0-9_]{1,32}$`)

    // templates compiled once (auto-escaped)
    tmpl = template.Must(template.New("pages").ParseGlob("templates/*.html"))
)

// tiny in-memory templates fallback (used if no templates dir present)
var (
    loginTemplate = template.Must(template.New("login").Parse(`
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Login</title></head>
<body>
<form action="/login_post" method="post">
  <p><input type="text" name="username" maxlength="32" required></p>
  <p><input type="password" name="password" required></p>
  <input type="hidden" name="csrf_token" value="{{.CSRF}}">
  <p><button type="submit">Submit</button></p>
</form>
</body>
</html>`))

    settingsTemplate = template.Must(template.New("settings").Parse(`
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Settings</title></head>
<body>
<h1>Secret settings for {{.Username}}</h1>
<p>Only visible after authentication.</p>
</body>
</html>`))
)

func init() {
    // read session key from environment (must be 32 or 64 bytes recommended)
    key := os.Getenv("SESSION_KEY")
    if key == "" {
        // Fallback: generate ephemeral key (not for production). Log warning.
        log.Println("WARNING: SESSION_KEY not set; generating ephemeral key. Use env SESSION_KEY in production.")
        k := securecookie.GenerateRandomKey(32)
        if k == nil {
            log.Fatal("failed to generate random key")
        }
        sCookie = securecookie.New(k, nil)
    } else {
        // use provided key bytes (base64 if long)
        var secret []byte
        decoded, err := base64.StdEncoding.DecodeString(key)
        if err == nil && len(decoded) >= 16 {
            secret = decoded
        } else {
            secret = []byte(key)
        }
        sCookie = securecookie.New(secret, nil)
    }
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // enforce authentication for settings
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("Starting server on :8080")
    // NOTE: production must run behind TLS (HTTPS). This example listens HTTP for local testing.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// set a set of common security headers for responses
func setSecurityHeaders(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")
    w.Header().Set("Content-Security-Policy", "default-src 'self'; object-src 'none'; base-uri 'self';")
    if r.TLS != nil {
        // only set HSTS if served over TLS
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }
}

// ensureLoggedIn decorator equivalent — checks signed session cookie
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        setSecurityHeaders(w, r)

        if username, ok := getSessionUsername(r); !ok || username == "" {
            // not authenticated
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// getSessionUsername returns username stored in signed cookie (if valid)
func getSessionUsername(r *http.Request) (string, bool) {
    c, err := r.Cookie("session")
    if err != nil {
        return "", false
    }
    var val map[string]string
    if err := sCookie.Decode("session", c.Value, &val); err != nil {
        return "", false
    }
    return val["username"], true
}

// createSession sets a signed session cookie
func createSession(w http.ResponseWriter, username string, r *http.Request) error {
    value := map[string]string{
        "username": username,
        "iat":      time.Now().UTC().Format(time.RFC3339),
    }
    encoded, err := sCookie.Encode("session", value)
    if err != nil {
        return err
    }
    cookie := &http.Cookie{
        Name:     "session",
        Value:    encoded,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteLaxMode,
        // set Secure only when TLS present; in production serve HTTPS and set true
        Secure: r.TLS != nil,
        // short-lived session for demo; production choose appropriate lifetime
        MaxAge: 3600,
    }
    http.SetCookie(w, cookie)
    return nil
}

// generateCSRFToken creates a random token and returns it base64-encoded
func generateCSRFToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// setCSRF sets a signed csrf cookie
func setCSRF(w http.ResponseWriter, token string, r *http.Request) error {
    encoded, err := sCookie.Encode("csrf", map[string]string{"token": token})
    if err != nil {
        return err
    }
    cookie := &http.Cookie{
        Name:     "csrf",
        Value:    encoded,
        Path:     "/",
        HttpOnly: true, // double-submit via cookie + hidden field; cookie HttpOnly prevents JS access
        SameSite: http.SameSiteLaxMode,
        Secure:   r.TLS != nil,
        MaxAge:   3600,
    }
    http.SetCookie(w, cookie)
    return nil
}

// validateCSRF compares form token with signed cookie token
func validateCSRF(r *http.Request) bool {
    form := r.FormValue("csrf_token")
    if form == "" {
        return false
    }
    c, err := r.Cookie("csrf")
    if err != nil {
        return false
    }
    var val map[string]string
    if err := sCookie.Decode("csrf", c.Value, &val); err != nil {
        return false
    }
    return form == val["token"]
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    // simple page
    _, _ = w.Write([]byte(`<html>Hello! <a href='./login'>Login here</a></html>`))
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)

    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // generate CSRF token and set signed cookie
    token, err := generateCSRFToken()
    if err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
    if err := setCSRF(w, token, r); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    // render inline template with token
    _ = loginTemplate.Execute(w, map[string]string{"CSRF": token})
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)

    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // CSRF protection
    if !validateCSRF(r) {
        http.Error(w, "Invalid CSRF token", http.StatusForbidden)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // input validation: whitelist usernames
    if !usernameRe.MatchString(username) {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Authenticate: compare against bcrypt hash stored in env
    pwHash := os.Getenv("ADMIN_PW_HASH")
    if pwHash == "" {
        // fallback: precomputed bcrypt hash of "password" (DEMO ONLY). Require env in prod.
        // Hash generated with bcrypt cost 12.
        pwHash = "$2a$12$wE7k0s0ZQmJ2o3x3dV2CZuV6n1cYxpoH4Qe1T4bY1qk8kz.9v8QpW"
    }

    if err := bcrypt.CompareHashAndPassword([]byte(pwHash), []byte(password)); err != nil {
        // don't reveal which part failed
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // Only allow the configured admin username (configurable via env ADMIN_USERNAME)
    adminUser := os.Getenv("ADMIN_USERNAME")
    if adminUser == "" {
        adminUser = "admin"
    }
    if username != adminUser {
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // create signed session cookie
    if err := createSession(w, username, r); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }

    // redirect to settings
    http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page (requires authentication)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    setSecurityHeaders(w, r)
    w.Header().Set("Content-Type", "text/html; charset=utf-8")

    username, ok := getSessionUsername(r)
    if !ok || username == "" {
        http.Error(w, "Forbidden", http.StatusForbidden)
        return
    }

    // render safe template with escaped username
    data := struct {
        Username string
    }{
        Username: username,
    }
    // Use our inline template; in real app, use template files and CSRF-protected forms.
    if err := settingsTemplate.Execute(w, data); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
}
