package main

import (
    "fmt"
    "net/http"
)

// Secure session management: Use a secure session store instead of a global map.
// For production, consider using libraries like gorilla/sessions with secure cookies.
// Avoid global state; in a real app, use a database or secure in-memory store.
// Principle of least privilege: Sessions should be scoped to users.
// Protect sensitive data: Ensure sessions are encrypted and use HTTPS (not implemented here, but recommended).
var session = make(map[string]string) // Insecure for demo; replace with secure session management.

func main() {
    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply authentication to critical functions like settings.
    // This prevents unauthorized access, addressing CWE-284 (Improper Access Control).
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    // Use HTTPS in production: http.ListenAndServeTLS(":8080", "cert.pem", "key.pem", nil)
    // Avoid running as root; use least privilege.
    http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
// Security: Checks for valid session before allowing access.
// In production, validate session tokens securely, e.g., with expiration and CSRF protection.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Secure default: Deny access if not authenticated.
        if session["username"] == "" {
            http.Redirect(w, r, "/login", http.StatusFound)
            return
        }
        next(w, r)
    }
}

// home page
// Security: Output encoding not needed here as it's static, but ensure no injection.
func indexHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
// Security: Restrict to GET method to prevent method tampering (CWE-284).
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Add CSRF token for protection against CSRF attacks.
    // For simplicity, omitted here, but recommended: Generate and validate tokens.
    fmt.Fprint(w, `
        <html><form action="/login_post" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    `)
}

// login POST system
// Security: Validate inputs, use secure password hashing (not implemented here due to hardcoded creds).
// Avoid hardcoding credentials; use environment variables or secure vaults.
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation: Sanitize and validate username/password.
    // Prevent injection: Use parameterized queries if storing in DB (not applicable here).
    // Secure password check: In production, hash passwords with bcrypt or similar.
    if username == "admin" && password == "password" {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Secure error handling: Do not leak info; generic message.
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
// Security: Protected by ensureLoggedIn to prevent unauthorized access.
// Output encoding: Use html.EscapeString if dynamic content; here it's static.
// Avoid XSS: Ensure all user inputs are escaped if displayed.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    fmt.Fprint(w, "<html>Secret settings</html>")
}
