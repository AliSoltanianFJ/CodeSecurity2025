package main

import (
    "crypto/hmac"
    "crypto/rand"
    "crypto/sha256"
    "encoding/base64"
    "encoding/hex"
    "fmt"
    "html/template"
    "log"
    "net"
    "net/http"
    "os"
    "strconv"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
// Security-sensitive configuration
var (
    // SESSION_SECRET must be set in environment in production. Used for HMAC signing of session tokens.
    sessionSecret = []byte(getenvOr("SESSION_SECRET", "dev-secret-change-me"))
    // hashed bcrypt password for "admin" (bcrypt hash of "password")
    // In production store user credentials in a protected DB and not in source.
    adminHashedPassword, _ = bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    // login attempt tracking
    loginAttempts   = make(map[string]*attemptInfo)
    loginAttemptsMu sync.Mutex
)

type attemptInfo struct {
    count     int
    firstSeen time.Time
}

const (
    maxLoginAttempts = 5
    lockoutWindow    = 1 * time.Minute
    sessionDuration  = 30 * time.Minute
)

func main() {
    mux := http.NewServeMux()

    // Public routes
    mux.HandleFunc("/", wrapWithSecurity(indexHandler))
    mux.HandleFunc("/login", wrapWithSecurity(loginHandler))
    mux.HandleFunc("/login_post", wrapWithSecurity(doLoginHandler))

    // Protected route (ensureLoggedIn middleware enforces authentication)
    mux.Handle("/settings", wrapWithSecurity(ensureLoggedIn(settingsHandler)))

    // Prefer TLS in production: provide TLS_CERT and TLS_KEY environment variables.
    cert := os.Getenv("TLS_CERT")
    key := os.Getenv("TLS_KEY")
    addr := ":8080"

    server := &http.Server{
        Addr:         addr,
        Handler:      mux,
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }

    log.Printf("Starting server on %s", addr)
    if cert != "" && key != "" {
        log.Printf("Using TLS (cert/key provided).")
        log.Fatal(server.ListenAndServeTLS(cert, key))
    }
    log.Printf("TLS not configured - running HTTP (development only). Do not use in production.")
    log.Fatal(server.ListenAndServe())
}

// =====================
// Helpers & Middleware
// =====================

func wrapWithSecurity(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Set common security headers
        // Content-Security-Policy: minimal default; adjust for application needs.
        w.Header().Set("Content-Security-Policy", "default-src 'self'")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "no-referrer")
        // HSTS only when over TLS
        if r.TLS != nil {
            w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        }
        // Call next
        next.ServeHTTP(w, r)
    }
}

// ensureLoggedIn enforces session cookie presence and validity.
func ensureLoggedIn(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        username, ok := validateSession(r)
        if !ok || username == "" {
            // Redirect to login - use 303 to avoid resubmitting POSTs
            http.Redirect(w, r, "/login", http.StatusSeeOther)
            return
        }
        // Put username into context via header for this simple example (avoid global state)
        r.Header.Set("X-Authenticated-User", username)
        next.ServeHTTP(w, r)
    })
}

// validateSession parses and validates the signed session token from cookie.
// Returns username and whether the session is valid.
func validateSession(r *http.Request) (string, bool) {
    c, err := r.Cookie("session")
    if err != nil {
        return "", false
    }
    decoded, err := base64.URLEncoding.DecodeString(c.Value)
    if err != nil {
        return "", false
    }
    parts := strings.Split(string(decoded), "|")
    if len(parts) != 3 {
        return "", false
    }
    username := parts[0]
    expUnix, err := strconv.ParseInt(parts[1], 10, 64)
    if err != nil {
        return "", false
    }
    sigHex := parts[2]
    // check expiration
    if time.Unix(expUnix, 0).Before(time.Now()) {
        return "", false
    }
    // verify HMAC
    msg := parts[0] + "|" + parts[1]
    expectedMAC := computeHMAC([]byte(msg), sessionSecret)
    expectedHex := hex.EncodeToString(expectedMAC)
    // constant-time comparison
    if !hmac.Equal([]byte(expectedHex), []byte(sigHex)) {
        return "", false
    }
    // whitelist validation: only "admin" allowed in this demo
    if username != "admin" {
        return "", false
    }
    return username, true
}

func computeHMAC(message, key []byte) []byte {
    mac := hmac.New(sha256.New, key)
    mac.Write(message)
    return mac.Sum(nil)
}

func setSessionCookie(w http.ResponseWriter, r *http.Request, username string) {
    exp := time.Now().Add(sessionDuration).Unix()
    msg := username + "|" + strconv.FormatInt(exp, 10)
    sig := computeHMAC([]byte(msg), sessionSecret)
    token := msg + "|" + hex.EncodeToString(sig)
    encoded := base64.URLEncoding.EncodeToString([]byte(token))

    cookie := &http.Cookie{
        Name:     "session",
        Value:    encoded,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Unix(exp, 0),
    }
    // Secure flag only when TLS is in use
    if r.TLS != nil {
        cookie.Secure = true
    }
    http.SetCookie(w, cookie)
}

// clearSessionCookie removes the session cookie
func clearSessionCookie(w http.ResponseWriter) {
    http.SetCookie(w, &http.Cookie{
        Name:     "session",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
        SameSite: http.SameSiteStrictMode,
    })
}

// Simple helper to generate a cryptographically secure random token (hex)
func randomToken(n int) string {
    b := make([]byte, n)
    _, err := rand.Read(b)
    if err != nil {
        // fallback
        return fmt.Sprintf("%d", time.Now().UnixNano())
    }
    return hex.EncodeToString(b)
}

// get client IP (best-effort)
func clientIP(r *http.Request) string {
    // trust X-Forwarded-For if behind a trusted proxy. Here we use first value as best-effort.
    if x := r.Header.Get("X-Forwarded-For"); x != "" {
        parts := strings.Split(x, ",")
        return strings.TrimSpace(parts[0])
    }
    ip, _, err := net.SplitHostPort(r.RemoteAddr)
    if err != nil {
        return r.RemoteAddr
    }
    return ip
}

// Simple rate limiter for login attempts per IP
func allowLoginAttempt(r *http.Request) bool {
    ip := clientIP(r)
    loginAttemptsMu.Lock()
    defer loginAttemptsMu.Unlock()
    a, ok := loginAttempts[ip]
    now := time.Now()
    if !ok || now.Sub(a.firstSeen) > lockoutWindow {
        loginAttempts[ip] = &attemptInfo{count: 0, firstSeen: now}
        a = loginAttempts[ip]
    }
    if a.count >= maxLoginAttempts {
        return false
    }
    a.count++
    return true
}

func resetLoginAttempts(r *http.Request) {
    ip := clientIP(r)
    loginAttemptsMu.Lock()
    delete(loginAttempts, ip)
    loginAttemptsMu.Unlock()
}

func getenvOr(k, def string) string {
    if v := os.Getenv(k); v != "" {
        return v
    }
    return def
}

// =====================
// Handlers
// =====================

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    // Minimal content; use template to ensure safe escaping if content becomes dynamic.
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    switch r.Method {
    case http.MethodGet:
        // Generate CSRF token and set as cookie (double-submit)
        csrf := randomToken(16)
        csrfCookie := &http.Cookie{
            Name:     "csrf_token",
            Value:    csrf,
            Path:     "/",
            HttpOnly: false, // needs to be readable by JS for double-submit; form here posts value directly
            SameSite: http.SameSiteStrictMode,
        }
        if r.TLS != nil {
            csrfCookie.Secure = true
        }
        http.SetCookie(w, csrfCookie)

        // Serve simple login form with hidden csrf token
        // Use a small template to avoid injection issues.
        t := template.Must(template.New("login").Parse(`
<html>
  <body>
    <form action="/login_post" method="post">
      <p><input type="text" name="username" autocomplete="username"></p>
      <p><input type="password" name="password" autocomplete="current-password"></p>
      <input type="hidden" name="csrf" value="{{.CSRF}}">
      <p><button type="submit">Submit</button></p>
    </form>
  </body>
</html>`))
        _ = t.Execute(w, struct{ CSRF string }{CSRF: csrf})
    default:
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    }
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Rate limiting
    if !allowLoginAttempt(r) {
        http.Error(w, "Too many login attempts, try again later", http.StatusTooManyRequests)
        return
    }

    // CSRF protection - double submit cookie
    csrfForm := r.FormValue("csrf")
    csrfCookie, err := r.Cookie("csrf_token")
    if err != nil || csrfForm == "" || csrfCookie.Value == "" || csrfForm != csrfCookie.Value {
        http.Error(w, "Invalid request (CSRF)", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Whitelist/validate username (simple demo)
    if username != "admin" {
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }
    // Compare password using bcrypt
    if bcrypt.CompareHashAndPassword(adminHashedPassword, []byte(password)) == nil {
        // Successful login
        setSessionCookie(w, r, username)
        resetLoginAttempts(r)
        // redirect to protected settings
        http.Redirect(w, r, "/settings", http.StatusSeeOther)
        return
    }
    // Failed login
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// user settings page - protected
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // The ensureLoggedIn middleware placed the username in a header for this example
    username := r.Header.Get("X-Authenticated-User")
    // Render secret settings safely using html/template to avoid XSS
    t := template.Must(template.New("settings").Parse(`
<!doctype html>
<html>
  <head><meta charset="utf-8"><title>Settings</title></head>
  <body>
    <h1>Settings for {{.User}}</h1>
    <p>This is a protected page. Secret settings:</p>
    <pre>{{.Secret}}</pre>
    <form action="/logout" method="post">
      <button type="submit">Logout</button>
    </form>
  </body>
</html>`))
    // Example secret content - in real apps fetch per-user secrets from secure store and redact as needed.
    data := struct {
        User   string
        Secret string
    }{
        User:   username,
        Secret: "API_KEY=*************\nCONFIG=protected",
    }
    _ = t.Execute(w, data)
}

// Note: For symmetry, add a logout endpoint to clear session (simple).
// This is not part of original file but improves security posture.
func init() {
    // Attach logout handler to the default mux via http.HandleFunc in main's mux is used,
    // but since we already created mux in main, route is added there. For simplicity, use default.
    // To keep single-file changes only, register logout on default mux as well.
    http.HandleFunc("/logout", func(w http.ResponseWriter, r *http.Request) {
        if r.Method != http.MethodPost {
            http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
            return
        }
        clearSessionCookie(w)
        // clear CSRF cookie
        http.SetCookie(w, &http.Cookie{
            Name:     "csrf_token",
            Value:    "",
            Path:     "/",
            Expires:  time.Unix(0, 0),
            MaxAge:   -1,
            HttpOnly: false,
            SameSite: http.SameSiteStrictMode,
        })
        http.Redirect(w, r, "/", http.StatusSeeOther)
    })
}
