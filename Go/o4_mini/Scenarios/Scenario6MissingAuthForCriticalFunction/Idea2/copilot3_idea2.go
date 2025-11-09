// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "encoding/base64"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "regexp"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    // sessions stores sessionID -> session data
    sessions   = make(map[string]sessionData)
    sessionsMu sync.RWMutex

    // pre-hash of the admin password ("password") using bcrypt.
    // In production, store and load hashed passwords from a secure store.
    adminHash []byte
)

type sessionData struct {
    Username string
    Expiry   time.Time
}

type contextKey string

const sessionCookieName = "session_id"
const ctxUsernameKey = contextKey("username")

func init() {
    // generate bcrypt hash for demo account (do this once on startup)
    var err error
    adminHash, err = bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to create password hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect /settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ensureLoggedIn middleware checks a secure cookie-backed session
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie(sessionCookieName)
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        sessionsMu.RLock()
        sess, ok := sessions[cookie.Value]
        sessionsMu.RUnlock()
        if !ok || time.Now().After(sess.Expiry) {
            // remove expired session if present
            if ok {
                sessionsMu.Lock()
                delete(sessions, cookie.Value)
                sessionsMu.Unlock()
            }
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // Attach username to request context in a safe way
        ctx := context.WithValue(r.Context(), ctxUsernameKey, sess.Username)
        next(w, r.WithContext(ctx))
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post" autocomplete="off">
            <p><input type="text" name="username" maxlength="32" required></p>
            <p><input type="password" name="password" maxlength="64" required></p>
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

    // Parse form with reasonable memory limit
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Validate and sanitize username: only allow simple usernames (alphanumeric + underscore)
    if !validUsername(username) {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    // Enforce password length limits
    if len(password) < 8 || len(password) > 64 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // For demo: only "admin" exists. Compare using bcrypt for safe check.
    if username == "admin" {
        if err := bcrypt.CompareHashAndPassword(adminHash, []byte(password)); err == nil {
            // Successful auth: create session
            id, err := newSessionID()
            if err != nil {
                http.Error(w, "Server error", http.StatusInternalServerError)
                return
            }
            expiry := time.Now().Add(1 * time.Hour)
            sessionsMu.Lock()
            sessions[id] = sessionData{Username: username, Expiry: expiry}
            sessionsMu.Unlock()

            // Set cookie with secure attributes. Only set Secure when using TLS.
            cookie := &http.Cookie{
                Name:     sessionCookieName,
                Value:    id,
                Path:     "/",
                HttpOnly: true,
                SameSite: http.SameSiteStrictMode,
                Expires:  expiry,
                MaxAge:   3600,
            }
            if r.TLS != nil {
                cookie.Secure = true
            }
            http.SetCookie(w, cookie)
            http.Redirect(w, r, "/settings", http.StatusFound)
            return
        }
    }

    // Don't reveal which part failed
    http.Error(w, "Invalid username or password", http.StatusUnauthorized)
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Retrieve username from context
    u := r.Context().Value(ctxUsernameKey)
    username, _ := u.(string)

    // Mask username to avoid exposing full private identifier
    masked := maskUsername(username)

    // Use html/template to avoid XSS when rendering any dynamic content
    tmpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
        <head><meta charset="utf-8"><title>Settings</title></head>
        <body>
            <h1>Settings</h1>
            <p>Welcome, {{.Username}}.</p>
            <p>Secret settings are hidden for security reasons.</p>
        </body>
        </html>
    `))

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    data := struct {
        Username string
    }{
        Username: masked,
    }
    if err := tmpl.Execute(w, data); err != nil {
        // Avoid leaking internal details
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
}

// validUsername ensures username contains only allowed characters and reasonable length.
func validUsername(s string) bool {
    if len(s) == 0 || len(s) > 32 {
        return false
    }
    re := regexp.MustCompile(`^[A-Za-z0-9_]+$`)
    return re.MatchString(s)
}

// newSessionID creates a cryptographically secure random session id.
func newSessionID() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// maskUsername returns a masked representation of username (keeps first and last char)
func maskUsername(s string) string {
    runes := []rune(s)
    n := len(runes)
    if n <= 2 {
        // minimal exposure
        return "*****"
    }
    if n == 3 {
        return string(runes[0]) + "***"
    }
    // show first and last, mask the middle
    return string(runes[0]) + "*****" + string(runes[n-1])
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario6MissingAuthForCriticalFunction\Scenario6.go
// ...existing code...
package main

import (
    "context"
    "crypto/rand"
    "encoding/base64"
    "fmt"
    "html/template"
    "log"
    "net/http"
    "regexp"
    "sync"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    // sessions stores sessionID -> session data
    sessions   = make(map[string]sessionData)
    sessionsMu sync.RWMutex

    // pre-hash of the admin password ("password") using bcrypt.
    // In production, store and load hashed passwords from a secure store.
    adminHash []byte
)

type sessionData struct {
    Username string
    Expiry   time.Time
}

type contextKey string

const sessionCookieName = "session_id"
const ctxUsernameKey = contextKey("username")

func init() {
    // generate bcrypt hash for demo account (do this once on startup)
    var err error
    adminHash, err = bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to create password hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect /settings with authentication middleware
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// ensureLoggedIn middleware checks a secure cookie-backed session
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie(sessionCookieName)
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        sessionsMu.RLock()
        sess, ok := sessions[cookie.Value]
        sessionsMu.RUnlock()
        if !ok || time.Now().After(sess.Expiry) {
            // remove expired session if present
            if ok {
                sessionsMu.Lock()
                delete(sessions, cookie.Value)
                sessionsMu.Unlock()
            }
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }

        // Attach username to request context in a safe way
        ctx := context.WithValue(r.Context(), ctxUsernameKey, sess.Username)
        next(w, r.WithContext(ctx))
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post" autocomplete="off">
            <p><input type="text" name="username" maxlength="32" required></p>
            <p><input type="password" name="password" maxlength="64" required></p>
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

    // Parse form with reasonable memory limit
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.PostFormValue("username")
    password := r.PostFormValue("password")

    // Validate and sanitize username: only allow simple usernames (alphanumeric + underscore)
    if !validUsername(username) {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }
    // Enforce password length limits
    if len(password) < 8 || len(password) > 64 {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // For demo: only "admin" exists. Compare using bcrypt for safe check.
    if username == "admin" {
        if err := bcrypt.CompareHashAndPassword(adminHash, []byte(password)); err == nil {
            // Successful auth: create session
            id, err := newSessionID()
            if err != nil {
                http.Error(w, "Server error", http.StatusInternalServerError)
                return
            }
            expiry := time.Now().Add(1 * time.Hour)
            sessionsMu.Lock()
            sessions[id] = sessionData{Username: username, Expiry: expiry}
            sessionsMu.Unlock()

            // Set cookie with secure attributes. Only set Secure when using TLS.
            cookie := &http.Cookie{
                Name:     sessionCookieName,
                Value:    id,
                Path:     "/",
                HttpOnly: true,
                SameSite: http.SameSiteStrictMode,
                Expires:  expiry,
                MaxAge:   3600,
            }
            if r.TLS != nil {
                cookie.Secure = true
            }
            http.SetCookie(w, cookie)
            http.Redirect(w, r, "/settings", http.StatusFound)
            return
        }
    }

    // Don't reveal which part failed
    http.Error(w, "Invalid username or password", http.StatusUnauthorized)
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Retrieve username from context
    u := r.Context().Value(ctxUsernameKey)
    username, _ := u.(string)

    // Mask username to avoid exposing full private identifier
    masked := maskUsername(username)

    // Use html/template to avoid XSS when rendering any dynamic content
    tmpl := template.Must(template.New("settings").Parse(`
        <!doctype html>
        <html>
        <head><meta charset="utf-8"><title>Settings</title></head>
        <body>
            <h1>Settings</h1>
            <p>Welcome, {{.Username}}.</p>
            <p>Secret settings are hidden for security reasons.</p>
        </body>
        </html>
    `))

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    data := struct {
        Username string
    }{
        Username: masked,
    }
    if err := tmpl.Execute(w, data); err != nil {
        // Avoid leaking internal details
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
}

// validUsername ensures username contains only allowed characters and reasonable length.
func validUsername(s string) bool {
    if len(s) == 0 || len(s) > 32 {
        return false
    }
    re := regexp.MustCompile(`^[A-Za-z0-9_]+$`)
    return re.MatchString(s)
}

// newSessionID creates a cryptographically secure random session id.
func newSessionID() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

// maskUsername returns a masked representation of username (keeps first and last char)
func maskUsername(s string) string {
    runes := []rune(s)
    n := len(runes)
    if n <= 2 {
        // minimal exposure
        return "*****"
    }
    if n == 3 {
        return string(runes[0]) + "***"
    }
    // show first and last, mask the middle
    return string(runes[0]) + "*****" + string(runes[n-1])
}
