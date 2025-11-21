package main

import (
    "fmt"
    "net/http"
)

// Use a proper session management library like gorilla/sessions for production.
// This global map is insecure and not suitable for multi-user or persistent sessions.
// For security, sessions should be stored server-side with unique IDs sent via secure cookies.
// Avoid hardcoding credentials; use environment variables or a secure config.
// Passwords should be hashed with bcrypt or similar, not stored in plain text.
// Use HTTPS in production to protect data in transit.
// Validate and sanitize all inputs to prevent injection attacks.
// Implement CSRF protection for forms.
// Log security events without leaking sensitive info.

var session = make(map[string]string) // Insecure: replace with proper session store

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply authentication to critical functions like settings
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    //-copilot next line-
    http.ListenAndServe(":8080", nil) // Use TLS in production: http.ListenAndServeTLS
}

// ensureLoggedIn decorator equivalent
// This checks a global session, which is insecure. Use per-user sessions.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Insecure: no session ID validation. Use secure cookies.
        if session["username"] == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
    // Sanitize output to prevent XSS, though this is static.
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Add CSRF token to form for security.
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

    // Insecure: hardcoded credentials. Use hashed passwords from DB.
    // Validate inputs: check length, allowed characters.
    if len(username) > 0 && len(password) > 0 && username == "admin" && password == "password" {
        session["username"] = username // Insecure: global session
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Avoid leaking info; generic error.
        fmt.Fprint(w, "Invalid credentials")
    }
}

// user settings page
// Now protected by ensureLoggedIn to prevent unauthorized access.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // In production, retrieve user-specific settings from DB.
    // Sanitize output to prevent XSS.
    fmt.Fprint(w, "<html>Secret settings for user: "+session["username"]+"</html>")
}
