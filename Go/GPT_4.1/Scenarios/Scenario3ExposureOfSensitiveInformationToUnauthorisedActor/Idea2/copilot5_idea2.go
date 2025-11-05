package main

import (
    "database/sql"
    "errors"
    "html/template"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// Simple HTML template for login (prevents XSS by escaping)
var loginTmpl = template.Must(template.New("login").Parse(`
<!DOCTYPE html>
<html>
<head><title>Login</title></head>
<body>
{{if .Error}}<p style="color:red;">{{.Error}}</p>{{end}}
<form method="POST" action="/login">
  Username: <input type="text" name="username" maxlength="50" required><br>
  Password: <input type="password" name="password" maxlength="50" required><br>
  <input type="submit" value="Login">
</form>
</body>
</html>
`))

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method == http.MethodGet {
        loginTmpl.Execute(w, nil)
        return
    }
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1024)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation and sanitization
    if !isValidUsername(username) || !isValidPassword(password) {
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password format."})
        return
    }

    // Use environment variables or a secure secret manager for credentials in production!
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared statements to prevent SQL injection
    var hashedPassword string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        // Do not reveal if user exists
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password."})
        return
    }

    // Compare hashed password
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password."})
        return
    }

    // Set secure session cookie (in production, use a secure session manager)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    generateSessionID(),
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // Set to true if using HTTPS
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(30 * time.Minute),
    })

    http.Redirect(w, r, "/dashboard", http.StatusSeeOther)
}

// Validate username: alphanumeric, 3-50 chars
func isValidUsername(u string) bool {
    re := regexp.MustCompile(`^[a-zA-Z0-9_]{3,50}$`)
    return re.MatchString(u)
}

// Validate password: 8-50 chars, no spaces
func isValidPassword(p string) bool {
    if len(p) < 8 || len(p) > 50 {
        return false
    }
    if regexp.MustCompile(`\s`).MatchString(p) {
        return false
    }
    return true
}

// Generate a random session ID (placeholder, use crypto/rand in production)
func generateSessionID() string {
    return "sessionplaceholder"
}
