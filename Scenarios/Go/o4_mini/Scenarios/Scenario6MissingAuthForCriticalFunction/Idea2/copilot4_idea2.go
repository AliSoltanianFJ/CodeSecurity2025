package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "regexp"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

// secure session store
var sessionStore = struct {
    sync.RWMutex
    m map[string]string
}{m: make(map[string]string)}

// admin credentials (hashed)
var adminHash []byte

func init() {
    // Pre-hash the known admin password securely once (do not log or expose)
    h, err := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to initialize password hash: %v", err)
    }
    adminHash = h
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_id")
        if err != nil || c.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // validate cookie format: hex string of expected length (64 chars for 32 bytes)
        if !regexp.MustCompile("^[a-f0-9]{64}$").MatchString(c.Value) {
            // invalid cookie -> remove and redirect
            http.SetCookie(w, &http.Cookie{
                Name:     "session_id",
                Value:    "",
                Path:     "/",
                MaxAge:   -1,
                HttpOnly: true,
            })
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        sessionStore.RLock()
        username, ok := sessionStore.m[c.Value]
        sessionStore.RUnlock()
        if !ok || username == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username in context if needed (omitted for brevity)
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
    // simple safe form; no user-controlled data included
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post" autocomplete="off">
            <p><input type="text" name="username" maxlength="32" required></p>
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

    // Parse form safely with size limits
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Validate and sanitize inputs: allow a conservative username pattern
    if !regexp.MustCompile(`^[A-Za-z0-9_\-]{3,32}$`).MatchString(username) {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    if len(password) < 8 || len(password) > 128 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Authenticate: compare against stored bcrypt hash
    if username == "admin" {
        if err := bcrypt.CompareHashAndPassword(adminHash, []byte(password)); err == nil {
            // successful login: create session
            sessionID, err := newSessionID(32)
            if err != nil {
                http.Error(w, "Internal server error", http.StatusInternalServerError)
                return
            }
            sessionStore.Lock()
            sessionStore.m[sessionID] = username
            sessionStore.Unlock()

            cookie := &http.Cookie{
                Name:     "session_id",
                Value:    sessionID,
                Path:     "/",
                HttpOnly: true,
                SameSite: http.SameSiteStrictMode,
                // set Secure only if request was over TLS
                Secure: r.TLS != nil,
                MaxAge: 3600,
            }
            http.SetCookie(w, cookie)
            http.Redirect(w, r, "/settings", http.StatusFound)
            return
        }
    }
    // Generic error message to avoid user enumeration
    http.Error(w, "Invalid username or password", http.StatusUnauthorized)
}

// user settings page (secured)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // retrieve session username
    c, err := r.Cookie("session_id")
    if err != nil || c.Value == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    sessionStore.RLock()
    username := sessionStore.m[c.Value]
    sessionStore.RUnlock()

    // defensive check
    if username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }

    // Use html/template to avoid XSS and escape username if displayed
    tmpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
          <head><meta charset="utf-8"><title>Settings</title></head>
          <body>
            <h1>Secret settings</h1>
            <p>Welcome, {{.Username}}.</p>
            <!-- Do not display or expose sensitive data such as session IDs or internal tokens -->
          </body>
        </html>
    `))

    data := struct {
        Username string
    }{
        Username: username,
    }

    // Execute template (escaping user content)
    if err := tmpl.Execute(w, data); err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
    }
}

// newSessionID returns a securely generated hex string of n bytes (2*n hex chars).
func newSessionID(n int) (string, error) {
    if n <= 0 || n > 1024 {
        return "", fmt.Errorf("invalid session id length")
    }
    b := make([]byte, n)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return hex.EncodeToString(b), nil
}
