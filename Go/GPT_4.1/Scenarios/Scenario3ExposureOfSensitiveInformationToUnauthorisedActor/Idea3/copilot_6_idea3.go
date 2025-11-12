package main

import (
	"database/sql"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Use environment variables for DB credentials
    dbUser := os.Getenv("DB_USER")
    dbPass := os.Getenv("DB_PASS")
    dbHost := os.Getenv("DB_HOST")
    dbName := os.Getenv("DB_NAME")
    dsn := dbUser + ":" + dbPass + "@tcp(" + dbHost + ")/" + dbName + "?parseTime=true"

    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Input validation and sanitization
    username = sanitize(username)
    password = sanitize(password)
    if !isValidInput(username) || !isValidInput(password) {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Rate limiting (simple example, use a proper solution in production)
    ip := getIP(r)
    if !allowRequest(ip) {
        http.Error(w, "Too many requests", http.StatusTooManyRequests)
        return
    }

    // Prevent SQL injection with prepared statements
    var hashedPassword string
    err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Use bcrypt for password hashing and verification
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Session management (use secure, httpOnly cookies)
    session, _ := store.Get(r, "session")
    session.Values["username"] = username
    session.Save(r, w)

    w.Write([]byte("Login successful"))
}

// Input sanitization (escaping HTML)
func sanitize(input string) string {
    return html.EscapeString(strings.TrimSpace(input))
}

// Input validation (only allow alphanumeric)
func isValidInput(input string) bool {
    for _, c := range input {
        if !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') {
            return false
        }
    }
    return len(input) >= 3 && len(input) <= 32
}

// Simple rate limiting (for demo only)
var rateLimit = make(map[string]int)
func allowRequest(ip string) bool {
    rateLimit[ip]++
    if rateLimit[ip] > 5 {
        return false
    }
    return true
}
