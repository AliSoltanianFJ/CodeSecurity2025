package main

import (
    "database/sql"
    "html/template"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var (
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)
    passwordRegex = regexp.MustCompile(`^[\S]{8,64}$`)
    loginTmpl     = template.Must(template.New("login").Parse(`
        <html><body>
        {{if .Error}}<div style="color:red">{{.Error}}</div>{{end}}
        <form method="POST" action="/login">
            Username: <input name="username" type="text" maxlength="32"/><br/>
            Password: <input name="password" type="password" maxlength="64"/><br/>
            <input type="submit" value="Login"/>
        </form>
        </body></html>
    `))
)

func main() {
    http.HandleFunc("/login", loginHandler)
    http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        loginTmpl.Execute(w, nil)
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

    // Input validation
    if !usernameRegex.MatchString(username) || !passwordRegex.MatchString(password) {
        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password format."})
        return
    }

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db?parseTime=true&charset=utf8mb4&tls=false")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Use prepared statement to prevent SQL injection
    var hashedPassword string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err == sql.ErrNoRows {
        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password."})
        return
    } else if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Compare password hash using bcrypt
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        w.Header().Set("Content-Type", "text/html; charset=utf-8")
        loginTmpl.Execute(w, map[string]string{"Error": "Invalid username or password."})
        return
    }

    // Set secure session cookie (example, not a full session implementation)
    http.SetCookie(w, &http.Cookie{
        Name:     "session_id",
        Value:    generateSessionID(),
        Path:     "/",
        HttpOnly: true,
        Secure:   false, // Set to true if using HTTPS
        SameSite: http.SameSiteStrictMode,
        Expires:  time.Now().Add(30 * time.Minute),
    })

    w.Header().Set("Content-Type", "text/html; charset=utf-8")
    w.Write([]byte("Login successful."))
}

// generateSessionID generates a random session ID (for demonstration only)
func generateSessionID() string {
    return time.Now().Format("20060102150405.000000")
}
