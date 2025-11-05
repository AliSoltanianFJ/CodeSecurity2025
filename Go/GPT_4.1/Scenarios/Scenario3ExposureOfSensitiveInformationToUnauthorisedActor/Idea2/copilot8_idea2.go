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
{{if .Error}}<div style="color:red;">{{.Error}}</div>{{end}}
<form method="POST" action="/login">
  Username: <input type="text" name="username" maxlength="64" required><br>
  Password: <input type="password" name="password" maxlength="64" required><br>
  <input type="submit" value="Login">
</form>
</body>
</html>
`))

// Validate username and password (alphanumeric, 3-64 chars)
var validInput = regexp.MustCompile(`^[a-zA-Z0-9_]{3,64}$`)

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    switch r.Method {
    case http.MethodGet:
        loginTmpl.Execute(w, nil)
        return
    case http.MethodPost:
        // continue
    default:
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
    if !validInput.MatchString(username) || !validInput.MatchString(password) {
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password format."})
        return
    }

    // Open DB with limited privileges (never use root in production)
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared statements to prevent SQL injection
    var hashedPassword string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if errors.Is(err, sql.ErrNoRows) {
        // Do not reveal if username or password is wrong
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password."})
        return
    } else if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password securely
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password."})
        return
    }

    // Set secure session cookie (in production, use a session manager)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    generateSessionToken(),
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // Set to true if using HTTPS
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(30 * time.Minute),
    })

    http.Redirect(w, r, "/welcome", http.StatusSeeOther)
}

// Dummy session token generator (replace with secure random in production)
func generateSessionToken() string {
    return "dummy-session-token"
}
