package main

import (
    "fmt"
    "net/http"
)

// Global session map - NOTE: In production, use a secure session store like gorilla/sessions
// to avoid race conditions, memory leaks, and lack of expiration. This is insecure for concurrent access.
var session = make(map[string]string)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply authentication to the critical /settings endpoint using the ensureLoggedIn decorator
    // This prevents unauthorized access, addressing CWE-306 (Missing Authentication for Critical Function)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil) // NOTE: Use HTTPS in production with TLS certificates to protect data in transit (CWE-319)
}

// ensureLoggedIn decorator equivalent - checks for valid session before allowing access
// Principle of least privilege: only authenticated users can access protected routes
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Check if session exists and is not empty
        if session["username"] == "" {
            // Redirect to login if not authenticated, preventing unauthorized access
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// home page - no sensitive data, safe to expose
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET) - validates method to prevent method tampering (CWE-352 potential mitigation)
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

// login POST system - validates method, sanitizes inputs implicitly via FormValue (but in production, use stricter validation)
// Hardcoded credentials are insecure (CWE-798); in production, use hashed passwords from a database
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // Basic authentication check - insecure due to hardcoded values; use bcrypt for hashing in production
    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Avoid leaking information about valid usernames; generic error message
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page - now protected by authentication
// Outputs user-specific data safely; in production, ensure no XSS via proper encoding
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Retrieve username from session for personalization
    username := session["username"]
    if username == "" {
        // This should not happen due to ensureLoggedIn, but defensive programming
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
        return
    }
    // Use fmt.Sprintf for safe string formatting to prevent injection
    fmt.Fprintf(w, "<html>Secret settings for user: %s</html>", username)
}
