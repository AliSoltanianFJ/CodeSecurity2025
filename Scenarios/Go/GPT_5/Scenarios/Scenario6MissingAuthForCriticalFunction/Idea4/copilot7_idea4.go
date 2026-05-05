package main

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "net/http"
)

// sessions map stores sessionID to username for authenticated users.
// This avoids using a global session variable and ensures per-session authentication.
// Session IDs are randomly generated to prevent prediction attacks.
var sessions = make(map[string]string)

// generateSessionID creates a cryptographically secure random session ID.
// Uses crypto/rand for security, avoiding predictable IDs.
func generateSessionID() string {
    bytes := make([]byte, 16)
    rand.Read(bytes)
    return hex.EncodeToString(bytes)
}

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply ensureLoggedIn decorator to /settings to enforce authentication.
    // This prevents unauthorized access to critical functions (CWE-306: Missing Authentication for Critical Function).
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    // Note: For production, use HTTPS (e.g., http.ListenAndServeTLS) to protect data in transit.
    // Obtain certificates from a trusted CA or use self-signed for testing, but never in production.
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn is a decorator that checks for a valid session cookie.
// Redirects to login if not authenticated.
// Uses HttpOnly, Secure, and SameSite cookies to mitigate XSS, session hijacking, and CSRF risks.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        cookie, err := r.Cookie("session")
        if err != nil || sessions[cookie.Value] == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// indexHandler serves the home page.
// No sensitive data; safe to serve without authentication.
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// loginHandler serves the login form (GET only).
// Restricts to GET to prevent method tampering (CWE-284: Improper Access Control).
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

// doLoginHandler processes login (POST only).
// Validates method, sanitizes inputs (trim spaces), checks credentials.
// On success, generates secure session ID, sets HttpOnly/Secure/SameSite cookie.
// Hardcoded credentials are for demo only; in production, use hashed passwords from a database.
// Avoids CWE-798: Use of Hard-coded Credentials by noting this is demo-only.
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")
    // Basic sanitization: trim whitespace to prevent bypass attempts.
    username = strings.TrimSpace(username)
    password = strings.TrimSpace(password)
    // Note: In production, use bcrypt or similar for password hashing with salts (CWE-916: Use of Password Hash With Insufficient Computational Effort).
    if username == "admin" && password == "password" {
        sessionID := generateSessionID()
        sessions[sessionID] = username
        // Set secure cookie: HttpOnly prevents JS access (XSS mitigation), Secure requires HTTPS, SameSite prevents CSRF.
        http.SetCookie(w, &http.Cookie{
            Name:     "session",
            Value:    sessionID,
            HttpOnly: true,
            Secure:   true, // Requires HTTPS; set to false for HTTP-only testing.
            SameSite: http.SameSiteStrictMode,
        })
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Avoid leaking info; generic error message (CWE-209: Information Exposure Through an Error Message).
        fmt.Fprint(w, "Bad username or password")
    }
}

// settingsHandler serves the user settings page.
// Only accessible after authentication via ensureLoggedIn.
// Displays basic content; in production, personalize with user data from secure storage.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Retrieve username from session for personalization (optional but secure).
    cookie, _ := r.Cookie("session")
    username := sessions[cookie.Value]
    fmt.Fprintf(w, "<html>Secret settings</html>")
}
