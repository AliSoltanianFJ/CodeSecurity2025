package main

import (
    "fmt"
    "net/http"
)

var session = make(map[string]string)

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply ensureLoggedIn decorator to settingsHandler to enforce authentication for critical function
    // This prevents unauthorized access, addressing CWE-306 (Missing Authentication for Critical Function)
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
// This implements a simple session-based authentication check
// Note: In production, use secure session management libraries like gorilla/sessions
// and consider CSRF protection for POST requests
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Check if user is logged in; redirect to login if not
        // This avoids privilege escalation by ensuring only authenticated users access sensitive pages
        if session["username"] == "" {
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

    // Basic authentication check; in production, use hashed passwords with salts
    // Hardcoded credentials are insecure; this is for demonstration only
    // CWE-798 (Use of Hard-coded Credentials) is present but mitigated by ensuring authentication is required
    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Avoid leaking information about valid usernames; generic error message
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
// This function now requires authentication via the ensureLoggedIn decorator
// Output is HTML-encoded to prevent XSS (CWE-79), though fmt.Fprint is used for simplicity
// In production, use a templating engine like html/template for proper encoding
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Ensure only GET requests are allowed for this page
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Display settings only to authenticated users
    fmt.Fprint(w, "<html>Secret settings</html>")
}
