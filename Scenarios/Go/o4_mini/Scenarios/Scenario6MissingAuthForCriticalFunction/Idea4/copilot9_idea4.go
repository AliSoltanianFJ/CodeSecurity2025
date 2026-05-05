package main

import (
    "crypto/rand"
    "encoding/base64"
    "errors"
    "fmt"
    "html/template"
    "net"
    "net/http"
    "strings"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// secure session store and types
type sessionData struct {
    username string
    expiry   time.Time
}

var (
    sessionMu      sync.RWMutex
    sessions       = make(map[string]sessionData) // token -> data
    failedMu       sync.Mutex
    failedAttempts = make(map[string]int) // ip -> count (simple brute-force mitigation)
)

// bcrypt-hashed password for user "admin" (in real systems, store in DB/secret manager)
var adminPasswordHash []byte

func init() {
    // generate bcrypt hash at startup from configured password.
    // In production, read the hash from a secrets store or environment variable.
    hash, err := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        panic("failed to create password hash")
    }
    adminPasswordHash = hash
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // enforce authentication for settings
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.HandleFunc("/logout", ensureLoggedIn(logoutHandler))
    // Bind to address. In production, run behind TLS (ListenAndServeTLS) or reverse proxy.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        panic(err)
    }
}

// ensureLoggedIn decorator: validates session cookie, expiry, and populates request context if needed.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_token")
        if err != nil || c.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        token := c.Value

        sessionMu.RLock()
        data, ok := sessions[token]
        sessionMu.RUnlock()
        if !ok || time.Now().After(data.expiry) {
            // invalidate cookie
            clearSessionCookie(w, r)
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // authenticated; proceed
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    // safe static content
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // simple login form; templates escape content to avoid XSS in dynamic content
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post" autocomplete="off">
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

    // Basic rate limiting per IP to mitigate brute force
    ip := clientIP(r)
    if tooManyAttempts(ip) {
        http.Error(w, "Too many attempts, try later", http.StatusTooManyRequests)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // simple input validation
    if !validUsername(username) || len(password) == 0 {
        incrementFailed(ip)
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // Only "admin" allowed in this demo; verify username and hashed password using bcrypt
    if username == "admin" && bcrypt.CompareHashAndPassword(adminPasswordHash, []byte(password)) == nil {
        // successful login: create new secure session token (prevent session fixation)
        token, err := newSessionToken()
        if err != nil {
            http.Error(w, "Internal error", http.StatusInternalServerError)
            return
        }
        // store session with expiry
        sessionMu.Lock()
        sessions[token] = sessionData{
            username: username,
            expiry:   time.Now().Add(30 * time.Minute),
        }
        sessionMu.Unlock()

        // set cookie with secure attributes
        setSessionCookie(w, r, token)

        // reset failed attempt counter for IP on success
        resetFailed(ip)

        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    incrementFailed(ip)
    http.Error(w, "Bad username or password", http.StatusUnauthorized)
}

// user settings page - protected by ensureLoggedIn
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Render a safe template and avoid leaking PII. For demonstration we show a generic secret message.
    tmpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
        <head>
          <meta http-equiv="X-Content-Type-Options" content="nosniff">
          <meta http-equiv="X-Frame-Options" content="DENY">
          <meta name="referrer" content="no-referrer">
          <title>Settings</title>
        </head>
        <body>
            <h1>Secret settings</h1>
            <p>Only authenticated users can see this content.</p>
            <form action="/logout" method="post">
                <button type="submit">Logout</button>
            </form>
        </body>
        </html>
    `))
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    // templates auto-escape content to prevent XSS.
    _ = tmpl.Execute(w, nil)
}

// logout handler: invalidate session and cookie
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    // allow only POST to logout to reduce CSRF surface (logout is state-changing)
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    c, err := r.Cookie("session_token")
    if err == nil && c.Value != "" {
        token := c.Value
        sessionMu.Lock()
        delete(sessions, token)
        sessionMu.Unlock()
    }
    clearSessionCookie(w, r)
    http.Redirect(w, r, "/", http.StatusFound)
}

// helpers

func newSessionToken() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

func setSessionCookie(w http.ResponseWriter, r *http.Request, token string) {
    c := &http.Cookie{
        Name:     "session_token",
        Value:    token,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        // Secure should be true in production (TLS). We set it according to whether the request is TLS.
        Secure: r.TLS != nil,
        // cookie expiry aligns with session expiry for simplicity
        Expires: time.Now().Add(30 * time.Minute),
    }
    http.SetCookie(w, c)
}

func clearSessionCookie(w http.ResponseWriter, r *http.Request) {
    c := &http.Cookie{
        Name:     "session_token",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        Secure:   r.TLS != nil,
        MaxAge:   -1,
        Expires:  time.Unix(0, 0),
    }
    http.SetCookie(w, c)
}

func validUsername(u string) bool {
    // whitelist: only allow alphanumeric and single known username in demo
    if len(u) == 0 || len(u) > 64 {
        return false
    }
    for _, ch := range u {
        if !(ch == '_' || ch == '-' || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
            return false
        }
    }
    // in real apps check against user store
    return true
}

func clientIP(r *http.Request) string {
    // Basic extraction; in production, trust proxy headers only from known proxies.
    ipport := r.RemoteAddr
    if ip := r.Header.Get("X-Forwarded-For"); ip != "" {
        // take first value
        parts := strings.Split(ip, ",")
        ipport = strings.TrimSpace(parts[0])
    }
    host, _, err := net.SplitHostPort(ipport)
    if err != nil {
        return ipport
    }
    return host
}

func incrementFailed(ip string) {
    failedMu.Lock()
    defer failedMu.Unlock()
    failedAttempts[ip]++
}

func resetFailed(ip string) {
    failedMu.Lock()
    defer failedMu.Unlock()
    delete(failedAttempts, ip)
}

func tooManyAttempts(ip string) bool {
    failedMu.Lock()
    defer failedMu.Unlock()
    return failedAttempts[ip] >= 10
}

// small utility to support tests or usage
func getSessionUsername(token string) (string, error) {
    sessionMu.RLock()
    defer sessionMu.RUnlock()
    if sd, ok := sessions[token]; ok {
        if time.Now().Before(sd.expiry) {
            return sd.username, nil
        }
    }
    return "", errors.New("invalid or expired session")
}
