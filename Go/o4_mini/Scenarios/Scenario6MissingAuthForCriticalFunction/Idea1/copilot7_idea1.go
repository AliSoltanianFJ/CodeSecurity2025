package main

import (
    "crypto/rand"
    "encoding/base64"
    "fmt"
    "html"
    "net/http"
    "time"
)

// ...existing code...

// simple in-memory session store mapping sessionID -> username
var sessions = make(map[string]string)

// a very small user store for demo purposes
var users = map[string]string{
    "admin": "password", // in a real app store a bcrypt hash instead
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // protect settings with the ensureLoggedIn wrapper
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        c, err := r.Cookie("session_id")
        if err != nil || c.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        username, ok := sessions[c.Value]
        if !ok || username == "" {
            // invalid session; clear cookie and redirect to login
            clearSessionCookie(w)
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        // attach username to request context if needed (omitted for brevity)
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
            <p><input type="text" name="username" required></p>
            <p><input type="password" name="password" required></p>
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

    // simple credential check; replace with hashed password comparison in real apps
    expected, ok := users[username]
    if !ok || expected != password {
        fmt.Fprint(w, "Bad username or password")
        return
    }

    // create a new session ID and set a cookie
    sessionID, err := generateSessionID()
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    sessions[sessionID] = username

    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    sessionID,
        Path:     "/",
        HttpOnly: true,
        SameSite: http.SameSiteStrictMode,
        // Secure should be true when serving over HTTPS. Set based on request:
        Secure:   r.TLS != nil,
        Expires:  time.Now().Add(24 * time.Hour),
    }
    http.SetCookie(w, cookie)
    http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page (protected)
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // by the time this handler runs, ensureLoggedIn validated the session cookie
    c, err := r.Cookie("session_id")
    if err != nil {
        http.Redirect(w, r, "/login", http.StatusFound)
        return
    }
    username := sessions[c.Value]
    // escape username before rendering to avoid XSS
    fmt.Fprintf(w, "<html>Secret settings for %s</html>", html.EscapeString(username))
}

// helper: generate a cryptographically secure random session id
func generateSessionID() (string, error) {
    b := make([]byte, 32)
    if _, err := rand.Read(b); err != nil {
        return "", err
    }
    return base64.RawURLEncoding.EncodeToString(b), nil
}

func clearSessionCookie(w http.ResponseWriter) {
    cookie := &http.Cookie{
        Name:     "session_id",
        Value:    "",
        Path:     "/",
        HttpOnly: true,
        Expires:  time.Unix(0, 0),
        MaxAge:   -1,
    }
    http.SetCookie(w, cookie)
}
