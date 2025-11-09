package main

import (
    "crypto/rand"
    "encoding/hex"
    "html/template"
    "log"
    "net/http"
    "strings"
    "sync"
    "unicode"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// secure in-memory session store
type sessionStore struct {
    mu   sync.RWMutex
    data map[string]string // sessionID -> username
}

var sessions = sessionStore{
    data: make(map[string]string),
}

// user store with bcrypt-hashed passwords (demo)
var users = map[string][]byte{}

func init() {
    // Pre-create a user "admin" with password "password" (bcrypt-hashed).
    // In real apps, use a proper user DB and salt+hash during user registration.
    hash, err := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to create password hash: %v", err)
    }
    users["admin"] = hash
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with the authentication wrapper
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// generate secure random session ID
func generateSessionID() (string, error) {
    const size = 32
    b := make([]byte, size)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}

func setSession(w http.ResponseWriter, r *http.Request, username string) error {
    sid, err := generateSessionID()
    if err != nil {
        return err
    }

    // store session
    sessions.mu.Lock()
    sessions.data[sid] = username
    sessions.mu.Unlock()

    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    sid,
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        // Secure should be set when serving over TLS
        Secure: r.TLS != nil,
        Path:   "/",
        // session cookie (no explicit MaxAge) — adjust as needed
    }
    http.SetCookie(w, cookie)
    return nil
}

func getUsernameFromRequest(r *http.Request) string {
    cookie, err := r.Cookie("session_id")
    if err != nil {
        return ""
    }
    sid := cookie.Value
    // basic validation of session id length/characters
    if len(sid) != 64 {
        return ""
    }
    // lookup
    sessions.mu.RLock()
    username := sessions.data[sid]
    sessions.mu.RUnlock()
    return username
}

// ensureLoggedIn decorator equivalent (uses cookie-backed session)
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        if getUsernameFromRequest(r) == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    // minimal content; links are safe literals
    w.Write([]byte(`<html>Hello! <a href='./login'>Login here</a></html>`))
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // If already logged in, redirect to settings
    if getUsernameFromRequest(r) != "" {
        http.Redirect(w, r, "/settings", http.StatusFound)
        return
    }

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte(`
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" maxlength="32" autocomplete="username"></p>
            <p><input type="password" name="password" maxlength="64" autocomplete="current-password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `))
}

// helper: validate and normalise username input
func normalizeAndValidateUsername(s string) (string, bool) {
    s = strings.TrimSpace(s)
    if s == "" || len(s) > 32 {
        return "", false
    }
    for _, r := range s {
        if !(unicode.IsLetter(r) || unicode.IsDigit(r) || r == '_' || r == '-') {
            return "", false
        }
    }
    return s, true
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // limit request body size to prevent large payloads
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MiB
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    usernameRaw := r.FormValue("username")
    password := r.FormValue("password")

    username, ok := normalizeAndValidateUsername(usernameRaw)
    if !ok || len(password) == 0 || len(password) > 256 {
        // generic error to avoid user enumeration
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // lookup user and verify password using bcrypt (constant-time)
    hashed, exists := users[username]
    if !exists {
        // don't reveal that user doesn't exist
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err := bcrypt.CompareHashAndPassword(hashed, []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // set session cookie
    if err := setSession(w, r, username); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page — protected by ensureLoggedIn wrapper
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := getUsernameFromRequest(r)
    if username == "" {
        // Should not happen because wrapper already enforces auth, but double-check.
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // Use html/template to ensure proper escaping and prevent XSS.
    tmpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
          <head><meta charset="utf-8"><title>Settings</title></head>
          <body>
            <h1>Settings</h1>
            <p>Welcome, {{.Username}}. Your secret settings are hidden for safety.</p>
            <form action="/logout" method="post"><button type="submit">Log out</button></form>
          </body>
        </html>
    `))

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    data := struct {
        Username string
    }{
        Username: username, // template will escape
    }
    _ = tmpl.Execute(w, data)
}
