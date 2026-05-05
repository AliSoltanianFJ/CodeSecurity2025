package main

import (
    "crypto/rand"
    "encoding/base64"
    "fmt"
    "html/template"
    "io"
    "log"
    "net/http"
    "regexp"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// Simple, concurrency-safe in-memory session store.
// Note: for production use a centralized store (Redis, DB) with expirations and cleanup.
type Session struct {
    Username  string
    CSRFToken string
    Expires   time.Time
}

type SessionStore struct {
    sync.RWMutex
    m map[string]Session
}

func (s *SessionStore) Set(id string, sess Session) {
    s.Lock()
    defer s.Unlock()
    s.m[id] = sess
}

func (s *SessionStore) Get(id string) (Session, bool) {
    s.RLock()
    defer s.RUnlock()
    sess, ok := s.m[id]
    // check expiration
    if ok && time.Now().After(sess.Expires) {
        ok = false
    }
    return sess, ok
}

func (s *SessionStore) Delete(id string) {
    s.Lock()
    defer s.Unlock()
    delete(s.m, id)
}

var sessions = &SessionStore{m: make(map[string]Session)}

// pre-computed bcrypt hash for demo admin password "password".
// In production, store hashed credentials in a secure database and never hardcode.
var adminHash []byte

var usernameRE = regexp.MustCompile(`^[A-Za-z0-9_]{3,32}$`)

var templates = template.Must(template.New("t").ParseGlob("templates/*.html"))
// For this standalone example we'll define templates inline if files missing.
var loginTmpl = template.Must(template.New("login").Parse(`
<html>
<head><meta charset="utf-8"></head>
<body>
<form action="/login_post" method="post">
    <p><input type="text" name="username" placeholder="username"></p>
    <p><input type="password" name="password" placeholder="password"></p>
    <input type="hidden" name="csrf" value="{{.CSRF}}">
    <p><button type="submit">Submit</button></p>
</form>
</body>
</html>
`))

var settingsTmpl = template.Must(template.New("settings").Parse(`
<html><head><meta charset="utf-8"></head><body>
<h1>Secret settings for {{.Username}}</h1>
<form method="post" action="/logout">
    <input type="hidden" name="csrf" value="{{.CSRF}}">
    <button type="submit">Logout</button>
</form>
</body></html>
`))

func main() {
    // prepare admin hash
    var err error
    adminHash, err = bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to generate password hash: %v", err)
    }

    // handlers with security headers middleware
    http.HandleFunc("/", secureHeaders(indexHandler))
    http.HandleFunc("/login", secureHeaders(loginHandler))
    http.HandleFunc("/login_post", secureHeaders(doLoginHandler))
    http.HandleFunc("/settings", secureHeaders(ensureLoggedIn(settingsHandler)))
    http.HandleFunc("/logout", secureHeaders(ensureLoggedIn(logoutHandler)))

    // NOTE: In production run behind TLS. Secure cookie flag is set conditionally when TLS present.
    log.Println("Listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatal(err)
    }
}

// secureHeaders sets common security response headers.
func secureHeaders(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Prevent MIME sniffing
        w.Header().Set("X-Content-Type-Options", "nosniff")
        // Clickjacking protection
        w.Header().Set("X-Frame-Options", "DENY")
        // Basic CSP - restrict scripts/styles; adjust as needed for your app
        w.Header().Set("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';")
        // Referrer policy
        w.Header().Set("Referrer-Policy", "no-referrer")
        // HSTS only when served over TLS
        if r.TLS != nil {
            w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        }
        next(w, r)
    }
}

// ensureLoggedIn decorator equivalent - checks session cookie and server-side session store.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_id")
        if err != nil || c.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        sess, ok := sessions.Get(c.Value)
        if !ok || sess.Username == "" {
            // invalid session
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username into context via request (simple approach: set header)
        r.Header.Set("X-Auth-Username", sess.Username)
        r.Header.Set("X-Auth-CSRF", sess.CSRFToken)
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    io.WriteString(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // generate a one-time CSRF token stored in a cookie for the login form
    csrf := randomToken(32)
    http.SetCookie(w, &http.Cookie{
        Name:     "login_csrf",
        Value:    csrf,
        Path:     "/",
        HttpOnly: true,
        // Secure should be true in production when using TLS; keep false for local dev HTTP
        Secure:   r.TLS != nil,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(10 * time.Minute),
    })
    // render login template with CSRF
    if err := loginTmpl.Execute(w, map[string]string{"CSRF": csrf}); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // limit request body to mitigate abuse
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }

    // Validate CSRF token (compare cookie and form)
    formCSRF := r.FormValue("csrf")
    cookie, err := r.Cookie("login_csrf")
    if err != nil || cookie.Value == "" || subtleConstantTimeCompare(cookie.Value, formCSRF) == false {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Strict whitelist for usernames
    if !usernameRE.MatchString(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        // minimal policy for demo; use stronger policies and rate limit in prod
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // For demo only we have a single admin user; compare bcrypt hash in constant time
    if username != "admin" || bcrypt.CompareHashAndPassword(adminHash, []byte(password)) != nil {
        // generic message to avoid user enumeration
        http.Error(w, "Bad username or password", http.StatusUnauthorized)
        return
    }

    // create server-side session: session ID and CSRF token
    sessionID := randomToken(32)
    sessionCSRF := randomToken(32)
    sessions.Set(sessionID, Session{
        Username:  username,
        CSRFToken: sessionCSRF,
        Expires:   time.Now().Add(24 * time.Hour),
    })

    // set session cookie
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        Secure:   r.TLS != nil, // must be true in production with TLS
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Now().Add(24 * time.Hour),
    })
    http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve username and CSRF token from header set in ensureLoggedIn
    username := r.Header.Get("X-Auth-Username")
    csrf := r.Header.Get("X-Auth-CSRF")
    // render using template (auto-escapes)
    data := struct {
        Username string
        CSRF     string
    }{
        Username: username,
        CSRF:     csrf,
    }
    if err := settingsTmpl.Execute(w, data); err != nil {
        http.Error(w, "Internal error", http.StatusInternalServerError)
        return
    }
}

// logout handler - demonstrates CSRF-checked state change
func logoutHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // limit body and parse
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Bad request", http.StatusBadRequest)
        return
    }
    // verify CSRF: get cookie session id and server-side CSRF
    c, err := r.Cookie("session_id")
    if err != nil || c.Value == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    sess, ok := sessions.Get(c.Value)
    if !ok {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    formCSRF := r.FormValue("csrf")
    if subtleConstantTimeCompare(sess.CSRFToken, formCSRF) == false {
        http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
        return
    }
    // delete server session and clear cookie
    sessions.Delete(c.Value)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Secure:   r.TLS != nil,
        SameSite: http.SameSiteLaxMode,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
    })
    http.Redirect(w, r, "/", http.StatusFound)
}

// randomToken returns a base64 URL-safe random string of length ~n bytes.
func randomToken(n int) string {
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        // fallback - should not happen
        return ""
    }
    return base64.RawURLEncoding.EncodeToString(b)
}

// subtleConstantTimeCompare avoids importing crypto/subtle directly each time.
func subtleConstantTimeCompare(a, b string) bool {
    // simple length check first
    if len(a) != len(b) {
        return false
    }
    // constant-time compare
    var res byte = 0
    for i := 0; i < len(a); i++ {
        res |= a[i] ^ b[i]
    }
    return res == 0
}