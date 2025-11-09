package main

import (
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "encoding/base64"
    "encoding/json"
    "encoding/hex"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "os"
    "strings"
    "time"
    "crypto/subtle"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// Simple signed session cookie format: base64(json) | hex(hmac)
type sessionData struct {
    Username string `json:"username"`
    Expiry   int64  `json:"expiry"` // unix seconds
    CSRF     string `json:"csrf,omitempty"`
}

var (
    // session signing key must come from environment in production
    sessionKey []byte

    // bcrypt hash for admin password; can be provided via ADMIN_PASSWORD_HASH env var
    adminPasswordHash []byte

    // template for login page
    loginTmpl = template.Must(template.New("login").Parse(`
<html>
    <head><meta charset="utf-8"><title>Login</title></head>
    <body>
        <form action="/login_post" method="post">
            <p><input type="text" name="username" autocomplete="username"></p>
            <p><input type="password" name="password" autocomplete="current-password"></p>
            <input type="hidden" name="csrf" value="{{.CSRF}}">
            <p><button type="submit">Submit</button></p>
        </form>
    </body>
</html>
`))

    // template for settings page
    settingsTmpl = template.Must(template.New("settings").Parse(`
<!doctype html>
<html>
 <head><meta charset="utf-8"><title>Settings</title></head>
 <body>
  <h1>Secret settings for {{.Username}}</h1>
  <p>Only authenticated users can see this page.</p>
  <form action="/logout" method="post">
    <input type="hidden" name="csrf" value="{{.CSRF}}">
    <button type="submit">Logout</button>
  </form>
 </body>
</html>
`))
)

func init() {
    // Load session key from env; require in production
    k := os.Getenv("SESSION_KEY")
    if k == "" {
        // fallback -- in real deployments fail fast. Here we create a random key to allow testing.
        tmp := make([]byte, 32)
        if _, err := rand.Read(tmp); err != nil {
            log.Fatalf("failed to generate session key: %v", err)
        }
        sessionKey = tmp
        log.Println("WARNING: SESSION_KEY not set; using ephemeral key suited only for testing")
    } else {
        sessionKey = []byte(k)
    }

    // Load bcrypt password hash from env or generate a default (for testing only)
    if h := os.Getenv("ADMIN_PASSWORD_HASH"); h != "" {
        adminPasswordHash = []byte(h)
    } else {
        // WARNING: default password is "password" for demo. Set ADMIN_PASSWORD_HASH in real deployments.
        hash, err := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
        if err != nil {
            log.Fatalf("failed to generate default password hash: %v", err)
        }
        adminPasswordHash = hash
        log.Println("WARNING: Using default password 'password'. Set ADMIN_PASSWORD_HASH in environment for production.")
    }
}

func main() {
    mux := http.NewServeMux()

    mux.HandleFunc("/", secureHeaders(indexHandler))
    mux.HandleFunc("/login", secureHeaders(loginHandler))
    mux.HandleFunc("/login_post", secureHeaders(doLoginHandler))
    mux.HandleFunc("/logout", secureHeaders(doLogoutHandler))
    // protect settings with authentication
    mux.HandleFunc("/settings", secureHeaders(ensureLoggedIn(settingsHandler)))

    // In production run with TLS (ListenAndServeTLS) and ensure SESSION_COOKIE is Secure.
    log.Println("Starting server on :8080")
    if err := http.ListenAndServe(":8080", mux); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// secureHeaders middleware adds security-related response headers
func secureHeaders(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Content Security Policy: basic restrictive policy; adjust for app needs.
        w.Header().Set("Content-Security-Policy", "default-src 'self'; object-src 'none'; frame-ancestors 'none'")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "no-referrer")
        // HSTS: only enable in production over TLS
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        next(w, r)
    }
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        username, _, ok := getSessionFromRequest(r)
        if !ok || username == "" {
            // no active session -> redirect to login
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username to context? for simplicity, set header for downstream use
        r.Header.Set("X-Auth-Username", username)
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
    // Generate CSRF token and set cookie (HttpOnly)
    csrf := generateToken(32)
    http.SetCookie(w, &http.Cookie{
        Name:     "csrf",
        Value:    csrf,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set true when running over TLS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(10 * time.Minute),
    })
    // Render login form with embedded CSRF value
    if err := loginTmpl.Execute(w, map[string]string{"CSRF": csrf}); err != nil {
        http.Error(w, "Template error", http.StatusInternalServerError)
    }
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // CSRF validation: compare submitted token to cookie token
    formCSRF := r.FormValue("csrf")
    cookie, err := r.Cookie("csrf")
    if err != nil || subtle.ConstantTimeCompare([]byte(formCSRF), []byte(cookie.Value)) != 1 {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic username normalization/whitelist (prevent injections and odd input)
    username = strings.TrimSpace(username)
    if username == "" || len(username) > 100 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Validate credentials using bcrypt; here only 'admin' is valid for demo,
    // in production validate against user store with least-privileged DB user.
    if username == "admin" {
        if err := bcrypt.CompareHashAndPassword(adminPasswordHash, []byte(password)); err == nil {
            // authentication success -> set signed session cookie
            if err := setSessionCookie(w, username); err != nil {
                http.Error(w, "Internal Server Error", http.StatusInternalServerError)
                return
            }
            http.Redirect(w, r, "/settings", http.StatusFound)
            return
        }
    }

    // generic error message to avoid username enumeration
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// logout handler: clears cookie
func doLogoutHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // CSRF validation for logout as well
    formCSRF := r.FormValue("csrf")
    cookie, err := r.Cookie("csrf")
    if err != nil || subtle.ConstantTimeCompare([]byte(formCSRF), []byte(cookie.Value)) != 1 {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }
    clearSessionCookie(w)
    http.Redirect(w, r, "/", http.StatusFound)
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    username, csrf, ok := getSessionFromRequest(r)
    if !ok {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    // Render settings using template to ensure HTML escaping
    data := map[string]string{
        "Username": username,
        "CSRF":     csrf,
    }
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Template error", http.StatusInternalServerError)
    }
}

// ---------- session and helper functions ----------

func setSessionCookie(w http.ResponseWriter, username string) error {
    s := sessionData{
        Username: username,
        Expiry:   time.Now().Add(30 * time.Minute).Unix(),
        CSRF:     generateToken(32),
    }
    j, err := json.Marshal(s)
    if err != nil {
        return err
    }
    b64 := base64.StdEncoding.EncodeToString(j)
    mac := computeHMAC([]byte(b64))
    val := b64 + "|" + hex.EncodeToString(mac)

    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    val,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set true when running over TLS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Unix(s.Expiry, 0),
    })
    // also set a csrf cookie (HttpOnly) for form validation
    http.SetCookie(w, &http.Cookie{
        Name:     "csrf",
        Value:    s.CSRF,
        Path:     "/",
        HttpOnly: true,
        Secure:   false,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Unix(s.Expiry, 0),
    })
    return nil
}

func clearSessionCookie(w http.ResponseWriter) {
    // expire cookies
    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
    })
    http.SetCookie(w, &http.Cookie{
        Name:     "csrf",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
    })
}

func getSessionFromRequest(r *http.Request) (username string, csrf string, ok bool) {
    c, err := r.Cookie("session")
    if err != nil {
        return "", "", false
    }
    parts := strings.SplitN(c.Value, "|", 2)
    if len(parts) != 2 {
        return "", "", false
    }
    b64 := parts[0]
    sigHex := parts[1]
    expectedMAC := computeHMAC([]byte(b64))
    sig, err := hex.DecodeString(sigHex)
    if err != nil {
        return "", "", false
    }
    // constant time compare
    if subtle.ConstantTimeCompare(expectedMAC, sig) != 1 {
        return "", "", false
    }
    j, err := base64.StdEncoding.DecodeString(b64)
    if err != nil {
        return "", "", false
    }
    var s sessionData
    if err := json.Unmarshal(j, &s); err != nil {
        return "", "", false
    }
    if time.Now().Unix() > s.Expiry {
        return "", "", false
    }
    return s.Username, s.CSRF, true
}

func computeHMAC(data []byte) []byte {
    m := hmac.New(sha256.New, sessionKey)
    _, _ = m.Write(data)
    return m.Sum(nil)
}

func generateToken(n int) string {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        // fallback to timestamp-based token (not ideal)
        return fmt.Sprintf("%d", time.Now().UnixNano())
    }
    return hex.EncodeToString(b)
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario6MissingAuthForCriticalFunction\Scenario6.go
package main

import (
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "encoding/base64"
    "encoding/json"
    "encoding/hex"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "os"
    "strings"
    "time"
    "crypto/subtle"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// Simple signed session cookie format: base64(json) | hex(hmac)
type sessionData struct {
    Username string `json:"username"`
    Expiry   int64  `json:"expiry"` // unix seconds
    CSRF     string `json:"csrf,omitempty"`
}

var (
    // session signing key must come from environment in production
    sessionKey []byte

    // bcrypt hash for admin password; can be provided via ADMIN_PASSWORD_HASH env var
    adminPasswordHash []byte

    // template for login page
    loginTmpl = template.Must(template.New("login").Parse(`
<html>
    <head><meta charset="utf-8"><title>Login</title></head>
    <body>
        <form action="/login_post" method="post">
            <p><input type="text" name="username" autocomplete="username"></p>
            <p><input type="password" name="password" autocomplete="current-password"></p>
            <input type="hidden" name="csrf" value="{{.CSRF}}">
            <p><button type="submit">Submit</button></p>
        </form>
    </body>
</html>
`))

    // template for settings page
    settingsTmpl = template.Must(template.New("settings").Parse(`
<!doctype html>
<html>
 <head><meta charset="utf-8"><title>Settings</title></head>
 <body>
  <h1>Secret settings for {{.Username}}</h1>
  <p>Only authenticated users can see this page.</p>
  <form action="/logout" method="post">
    <input type="hidden" name="csrf" value="{{.CSRF}}">
    <button type="submit">Logout</button>
  </form>
 </body>
</html>
`))
)

func init() {
    // Load session key from env; require in production
    k := os.Getenv("SESSION_KEY")
    if k == "" {
        // fallback -- in real deployments fail fast. Here we create a random key to allow testing.
        tmp := make([]byte, 32)
        if _, err := rand.Read(tmp); err != nil {
            log.Fatalf("failed to generate session key: %v", err)
        }
        sessionKey = tmp
        log.Println("WARNING: SESSION_KEY not set; using ephemeral key suited only for testing")
    } else {
        sessionKey = []byte(k)
    }

    // Load bcrypt password hash from env or generate a default (for testing only)
    if h := os.Getenv("ADMIN_PASSWORD_HASH"); h != "" {
        adminPasswordHash = []byte(h)
    } else {
        // WARNING: default password is "password" for demo. Set ADMIN_PASSWORD_HASH in real deployments.
        hash, err := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
        if err != nil {
            log.Fatalf("failed to generate default password hash: %v", err)
        }
        adminPasswordHash = hash
        log.Println("WARNING: Using default password 'password'. Set ADMIN_PASSWORD_HASH in environment for production.")
    }
}

func main() {
    mux := http.NewServeMux()

    mux.HandleFunc("/", secureHeaders(indexHandler))
    mux.HandleFunc("/login", secureHeaders(loginHandler))
    mux.HandleFunc("/login_post", secureHeaders(doLoginHandler))
    mux.HandleFunc("/logout", secureHeaders(doLogoutHandler))
    // protect settings with authentication
    mux.HandleFunc("/settings", secureHeaders(ensureLoggedIn(settingsHandler)))

    // In production run with TLS (ListenAndServeTLS) and ensure SESSION_COOKIE is Secure.
    log.Println("Starting server on :8080")
    if err := http.ListenAndServe(":8080", mux); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// secureHeaders middleware adds security-related response headers
func secureHeaders(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Content Security Policy: basic restrictive policy; adjust for app needs.
        w.Header().Set("Content-Security-Policy", "default-src 'self'; object-src 'none'; frame-ancestors 'none'")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "no-referrer")
        // HSTS: only enable in production over TLS
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        next(w, r)
    }
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        username, _, ok := getSessionFromRequest(r)
        if !ok || username == "" {
            // no active session -> redirect to login
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username to context? for simplicity, set header for downstream use
        r.Header.Set("X-Auth-Username", username)
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
    // Generate CSRF token and set cookie (HttpOnly)
    csrf := generateToken(32)
    http.SetCookie(w, &http.Cookie{
        Name:     "csrf",
        Value:    csrf,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set true when running over TLS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(10 * time.Minute),
    })
    // Render login form with embedded CSRF value
    if err := loginTmpl.Execute(w, map[string]string{"CSRF": csrf}); err != nil {
        http.Error(w, "Template error", http.StatusInternalServerError)
    }
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // CSRF validation: compare submitted token to cookie token
    formCSRF := r.FormValue("csrf")
    cookie, err := r.Cookie("csrf")
    if err != nil || subtle.ConstantTimeCompare([]byte(formCSRF), []byte(cookie.Value)) != 1 {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic username normalization/whitelist (prevent injections and odd input)
    username = strings.TrimSpace(username)
    if username == "" || len(username) > 100 {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }

    // Validate credentials using bcrypt; here only 'admin' is valid for demo,
    // in production validate against user store with least-privileged DB user.
    if username == "admin" {
        if err := bcrypt.CompareHashAndPassword(adminPasswordHash, []byte(password)); err == nil {
            // authentication success -> set signed session cookie
            if err := setSessionCookie(w, username); err != nil {
                http.Error(w, "Internal Server Error", http.StatusInternalServerError)
                return
            }
            http.Redirect(w, r, "/settings", http.StatusFound)
            return
        }
    }

    // generic error message to avoid username enumeration
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// logout handler: clears cookie
func doLogoutHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // CSRF validation for logout as well
    formCSRF := r.FormValue("csrf")
    cookie, err := r.Cookie("csrf")
    if err != nil || subtle.ConstantTimeCompare([]byte(formCSRF), []byte(cookie.Value)) != 1 {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }
    clearSessionCookie(w)
    http.Redirect(w, r, "/", http.StatusFound)
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    username, csrf, ok := getSessionFromRequest(r)
    if !ok {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    // Render settings using template to ensure HTML escaping
    data := map[string]string{
        "Username": username,
        "CSRF":     csrf,
    }
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Template error", http.StatusInternalServerError)
    }
}

// ---------- session and helper functions ----------

func setSessionCookie(w http.ResponseWriter, username string) error {
    s := sessionData{
        Username: username,
        Expiry:   time.Now().Add(30 * time.Minute).Unix(),
        CSRF:     generateToken(32),
    }
    j, err := json.Marshal(s)
    if err != nil {
        return err
    }
    b64 := base64.StdEncoding.EncodeToString(j)
    mac := computeHMAC([]byte(b64))
    val := b64 + "|" + hex.EncodeToString(mac)

    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    val,
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // set true when running over TLS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Unix(s.Expiry, 0),
    })
    // also set a csrf cookie (HttpOnly) for form validation
    http.SetCookie(w, &http.Cookie{
        Name:     "csrf",
        Value:    s.CSRF,
        Path:     "/",
        HttpOnly: true,
        Secure:   false,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Unix(s.Expiry, 0),
    })
    return nil
}

func clearSessionCookie(w http.ResponseWriter) {
    // expire cookies
    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
    })
    http.SetCookie(w, &http.Cookie{
        Name:     "csrf",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
    })
}

func getSessionFromRequest(r *http.Request) (username string, csrf string, ok bool) {
    c, err := r.Cookie("session")
    if err != nil {
        return "", "", false
    }
    parts := strings.SplitN(c.Value, "|", 2)
    if len(parts) != 2 {
        return "", "", false
    }
    b64 := parts[0]
    sigHex := parts[1]
    expectedMAC := computeHMAC([]byte(b64))
    sig, err := hex.DecodeString(sigHex)
    if err != nil {
        return "", "", false
    }
    // constant time compare
    if subtle.ConstantTimeCompare(expectedMAC, sig) != 1 {
        return "", "", false
    }
    j, err := base64.StdEncoding.DecodeString(b64)
    if err != nil {
        return "", "", false
    }
    var s sessionData
    if err := json.Unmarshal(j, &s); err != nil {
        return "", "", false
    }
    if time.Now().Unix() > s.Expiry {
        return "", "", false
    }
    return s.Username, s.CSRF, true
}

func computeHMAC(data []byte) []byte {
    m := hmac.New(sha256.New, sessionKey)
    _, _ = m.Write(data)
    return m.Sum(nil)
}

func generateToken(n int) string {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        // fallback to timestamp-based token (not ideal)
        return fmt.Sprintf("%d", time.Now().UnixNano())
    }
    return hex.EncodeToString(b)
}
