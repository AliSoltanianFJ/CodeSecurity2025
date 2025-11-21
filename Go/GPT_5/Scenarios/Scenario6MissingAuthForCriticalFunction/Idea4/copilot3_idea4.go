package main

import (
    "fmt"
    "html/template"
    "net/http"

    "golang.org/x/crypto/bcrypt"
)

// Global session map for simplicity; in production, use secure session management with expiration and secure cookies.
// Avoid global state; consider using a database or secure session store.
var session = make(map[string]string)

// Hashed password stored securely; in production, store in a database with proper access controls.
// Use bcrypt for password hashing to prevent plaintext storage and resist brute-force attacks.
// Principle of least privilege: only store necessary data.
var hashedPassword string

// Template for settings page to prevent XSS by using context-aware output encoding.
// Templates ensure HTML is properly escaped.
var settingsTmpl = template.Must(template.New("settings").Parse(`
<html>
<body>
<h1>Settings</h1>
<form action="/change_password" method="post">
    <p>Old Password: <input type="password" name="old_password" required></p>
    <p>New Password: <input type="password" name="new_password" required minlength="8"></p>
    <p><button type="submit">Change Password</button></p>
</form>
</body>
</html>
`))

func main() {
    // Initialize hashed password securely; in production, load from secure config or database.
    // Avoid hardcoding secrets; use environment variables or secure vaults.
    hashed, err := bcrypt.GenerateFromPassword([]byte("password"), bcrypt.DefaultCost)
    if err != nil {
        panic("Failed to hash password") // Proper error handling; in production, log and exit gracefully.
    }
    hashedPassword = string(hashed)

    http.HandleFunc("/", indexHandler)
    http.HandleFunc("/login", loginHandler)
    http.HandleFunc("/login_post", doLoginHandler)
    // Apply authentication to critical function to prevent unauthorized access (CWE-284: Improper Access Control).
    http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
    http.HandleFunc("/change_password", ensureLoggedIn(changePasswordHandler))
    // Use HTTPS in production to protect data in transit (CWE-319: Cleartext Transmission).
    http.ListenAndServe(":8080", nil) // In production, use TLS with certificates.
}

// ensureLoggedIn decorator equivalent for authentication.
// Checks session for logged-in user; redirects to login if not authenticated.
// Use secure session tokens with expiration to prevent session fixation (CWE-384).
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
    // Sanitize output; use templates if dynamic content is added.
    fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }
    // Hardcoded HTML is acceptable here as it's static; for dynamic, use templates to prevent XSS.
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

    // Validate inputs: check for empty values to prevent incomplete authentication.
    if username == "" || password == "" {
        fmt.Fprint(w, "Username and password required")
        return
    }

    // Use bcrypt for secure password verification; prevents timing attacks and plaintext exposure.
    if username == "admin" && bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) == nil {
        session["username"] = username
        http.Redirect(w, r, "/settings", http.StatusFound)
    } else {
        // Generic error message to avoid user enumeration (CWE-200: Information Exposure).
        fmt.Fprint(w, "Bad username or password")
    }
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
    // Execute template securely; templates handle XSS prevention.
    err := settingsTmpl.Execute(w, nil)
    if err != nil {
        // Proper error handling without leaking details (CWE-209: Information Exposure Through Error Messages).
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}

// Change password handler for settings.
func changePasswordHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    oldPass := r.FormValue("old_password")
    newPass := r.FormValue("new_password")

    // Validate inputs: ensure old password is correct and new password meets strength requirements.
    if oldPass == "" || newPass == "" {
        fmt.Fprint(w, "All fields required")
        return
    }

    // Verify old password securely.
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(oldPass)) != nil {
        fmt.Fprint(w, "Old password incorrect")
        return
    }

    // Enforce password strength: minimum length to resist brute-force (CWE-521: Weak Password Requirements).
    if len(newPass) < 8 {
        fmt.Fprint(w, "New password must be at least 8 characters")
        return
    }

    // Hash new password securely.
    hashed, err := bcrypt.GenerateFromPassword([]byte(newPass), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    hashedPassword = string(hashed)

    // Redirect after successful change; consider logging for audit.
    http.Redirect(w, r, "/settings", http.StatusFound)
}
