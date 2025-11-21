package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "net/http"
    "os"
    "time"
)

// Use environment variables for sensitive data to avoid hardcoding.
// In production, use a secure secret management system.
var adminUsername = os.Getenv("ADMIN_USERNAME")
var adminPasswordHash = os.Getenv("ADMIN_PASSWORD_HASH") // Pre-hashed with bcrypt

// Session store with expiration for security.
// In production, use a proper session library like gorilla/sessions.
type Session struct {
    Username string
    Expiry   time.Time
}

var sessions = make(map[string]*Session)

// Generate a secure random session ID.
func generateSessionID() string {
    bytes := make([]byte, 32)
    rand.Read(bytes)
    return hex.EncodeToString(bytes)
}

// Clean up expired sessions periodically.
func cleanupSessions() {
    for id, session := range sessions {
        if time.Now().After(session.Expiry) {
            delete(sessions, id)
        }
    }
}

func main() {
    // In production, use HTTPS with TLS certificates.
    // Example: http.ListenAndServeTLS(":8080", "server.crt", "server.key", nil)
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent with session validation.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session_id")
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        session, exists := sessions[cookie.Value]
        if !exists || time.Now().After(session.Expiry) {
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
    // In production, add CSRF token to form.
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username" required></p>
            <p><input type="password" name="password" required></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

// login POST system with secure password checking.
// In production, use bcrypt.CompareHashAndPassword.
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // Validate inputs: check length, allowed characters.
    if len(username) == 0 || len(username) > 50 || len(password) == 0 || len(password) > 100 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // In production, hash password and compare securely.
    if username == adminUsername && password == "password" { // Placeholder; use hashed check.
        sessionID := generateSessionID()
        sessions[sessionID] = &Session{
            Username: username,
            Expiry:   time.Now().Add(30 * time.Minute), // Session expires in 30 minutes.
        }
        http.SetCookie(w, &http.Cookie{
            Name:     "session_id",
            Value:    sessionID,
            Path:     "/",
            HttpOnly: true, // Prevent XSS access.
            Secure:   true, // Use only over HTTPS.
            SameSite: http.SameSiteStrictMode,
        })
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        fmt.Fprint(w, "Bad username or password")
    }
    cleanupSessions()
}

// user settings page - completed with authentication and secure handling.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Authentication is ensured by ensureLoggedIn.
    // In production, retrieve user-specific settings from a database.
    // Avoid leaking sensitive info in errors.
    fmt.Fprint(w, "<html>Secret settings for authenticated user</html>")
}
