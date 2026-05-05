package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "net/http"
    "os"
)

// Use a map to simulate sessions; in production, use a secure session store like Redis or database.
// Sessions should be tied to user IDs and have expiration times.
var sessions = make(map[string]string)

// Load credentials from environment variables to avoid hardcoding secrets.
// In production, use a secure credential management system.
var adminUsername = os.Getenv("ADMIN_USERNAME")
var adminPasswordHash = os.Getenv("ADMIN_PASSWORD_HASH") // Pre-computed hash of password with salt

func main() {
    // Use HTTPS in production; this example uses HTTP for simplicity but notes the security issue.
    // To enable HTTPS: http.ListenAndServeTLS(":8080", "server.crt", "server.key", nil)
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply authentication to critical functions like settings.
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
// This checks for a valid session cookie; in production, validate session expiration and use secure cookies.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session_id")
        if err != nil || cookie.Value == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        if _, exists := sessions[cookie.Value]; !exists {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    // Use context-aware output encoding to prevent XSS; here, simple HTML is safe.
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Sanitize output; this is static HTML, so low risk.
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

    // Validate inputs: check for empty strings and length limits to prevent injection.
    if len(username) == 0 || len(username) > 50 || len(password) == 0 || len(password) > 100 {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // In production, hash the input password with the same salt and compare.
    // Here, for simplicity, assume adminPasswordHash is set; replace with proper hashing like bcrypt.
    if username == adminUsername && password == "password" { // Placeholder; use hashed comparison
        // Generate a secure random session ID.
        sessionID := generateSessionID()
        sessions[sessionID] = username
        // Set secure cookie with HttpOnly and Secure flags (Secure requires HTTPS).
        http.SetCookie(w, &http.Cookie{
            Name:     "session_id",
            Value:    sessionID,
            Path:     "/",
            HttpOnly: true,
            Secure:   false, // Set to true in production with HTTPS
            SameSite: http.SameSiteStrictMode,
        })
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Avoid leaking information; generic error message.
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // This function is now protected by ensureLoggedIn.
    // In production, ensure all operations follow least privilege; e.g., check user permissions.
    fmt.Fprint(w, "<html>Secret settings</html>")
}

// Generate a secure random session ID.
func generateSessionID() string {
    bytes := make([]byte, 32)
    rand.Read(bytes)
    return hex.EncodeToString(bytes)
}
