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
    // Security decision: Protect the settings handler with authentication to prevent unauthorized access.
    // This addresses CWE-306 (Missing Authentication for Critical Function).
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
// Security decision: This middleware checks for session-based authentication.
// Note: In a production environment, use secure session management (e.g., with cookies and server-side storage)
// to avoid shared global state issues. Also, consider using HTTPS to protect session data in transit.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
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

    // Security decision: Hardcoded credentials are used here for simplicity, but in production,
    // avoid hardcoding secrets. Use environment variables or a secure configuration store.
    // Additionally, passwords should be hashed and compared securely (e.g., using bcrypt).
    // This addresses CWE-798 (Use of Hard-coded Credentials) partially; full mitigation requires
    // proper password storage.
    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Security decision: Avoid leaking information about valid usernames; use generic error messages.
        // This helps prevent user enumeration attacks.
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
// Security decision: This handler is now protected by ensureLoggedIn, ensuring only authenticated users can access it.
// In a real application, consider adding CSRF protection, input validation, and secure output encoding if dynamic content is added.
// Use HTTPS in production to protect data in transit (CWE-319).
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<html>Secret settings</html>")
}
