// ...existing code...
package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "log"
    "net/http"
    "sync"
    "time"
)

// simple server-side session store (thread-safe)
type sessionStore struct {
    mu sync.RWMutex
    m  map[string]string // sessionID -> username
}

var sessions = &sessionStore{m: make(map[string]string)}

// generate a cryptographically secure session ID and store the username
func newSession(username string) (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    id := hex.EncodeToString(b)
    sessions.mu.Lock()
    sessions.m[id] = username
    sessions.mu.Unlock()
    return id, nil
}

func getUsernameFromRequest(r *http.Request) string {
    c, err := r.Cookie("session_id")
    if err != nil {
        return ""
    }
    sessions.mu.RLock()
    username := sessions.m[c.Value]
    sessions.mu.RUnlock()
    return username
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect /settings with authentication check
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ensureLoggedIn decorator equivalent
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
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
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
    username := r.FormValue("username")
    password := r.FormValue("password")

    // placeholder authentication - replace with real auth in production
    if username == "admin" && password == "password" {
        id, err := newSession(username)
        if err != nil {
            http.Error(w, "internal server error", http.StatusInternalServerError)
            return
        }
        // Set a secure cookie for the session. In production set Secure: true when using HTTPS.
        http.SetCookie(w, &http.Cookie{
            Name:     "session_id",
            Value:    id,
            Path:     "/",
            HttpOnly: true,
            SameSite: http.SameSiteLaxMode,
            Expires:  time.Now().Add(24 * time.Hour),
        })
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    username := getUsernameFromRequest(r)
    // ensureLoggedIn wrapper should already have checked this, but double-check defensively
    if username == "" {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    fmt.Fprintf(w, "<html>Secret settings for %s</html>", username)
}
