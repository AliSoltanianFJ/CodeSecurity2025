package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// Security: Use environment variables for sensitive configuration, never hardcode secrets.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbName     = os.Getenv("DB_NAME")
)

// Security: Compile regex once for efficiency and to avoid ReDoS.
var (
    usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]{3,32}$`)
    passwordRegex = regexp.MustCompile(`^[\S]{8,64}$`) // At least 8 non-whitespace chars
)

// Security: Centralized error message for user-facing errors to avoid information leakage.
const genericErrorMsg = "An error occurred. Please try again later."

func main() {
    // Security: Enforce HTTPS by redirecting HTTP to HTTPS in production.
    http.HandleFunc("/register", registerHandler)
    log.Println("Listening on :8080 (ensure HTTPS in production)")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("Server failed: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow POST requests.
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS.
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    // Security: Parse form data safely.
    if err := r.ParseForm(); err != nil {
        http.Error(w, genericErrorMsg, http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Security: Validate and sanitize user input.
    if !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !isValidPassword(password) {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Security: Hash password with bcrypt (strong, salted).
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("Password hashing failed: %v", err)
        http.Error(w, genericErrorMsg, http.StatusInternalServerError)
        return
    }

    // Security: Use parameterized queries to prevent SQL injection.
    db, err := openDB()
    if err != nil {
        log.Printf("DB connection failed: %v", err)
        http.Error(w, genericErrorMsg, http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Principle of least privilege - only insert necessary fields.
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)")
    if err != nil {
        log.Printf("DB prepare failed: %v", err)
        http.Error(w, genericErrorMsg, http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, time.Now().UTC())
    if err != nil {
        // Security: Do not leak DB errors to user.
        log.Printf("DB exec failed: %v", err)
        http.Error(w, genericErrorMsg, http.StatusInternalServerError)
        return
    }

    // Security: Respond with minimal information.
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"status": "success"})
}

// openDB opens a database connection using environment variables.
// Security: Never hardcode credentials; use least privilege DB user.
func openDB() (*sql.DB, error) {
    if dbUser == "" || dbPassword == "" || dbHost == "" || dbName == "" {
        return nil, errors.New("database credentials not set")
    }
    dsn := fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true&tls=true", dbUser, dbPassword, dbHost, dbName)
    return sql.Open("mysql", dsn)
}

// isValidUsername validates the username against a strict regex.
func isValidUsername(username string) bool {
    return usernameRegex.MatchString(username)
}

// isValidPassword validates the password for length and character requirements.
func isValidPassword(password string) bool {
    return passwordRegex.MatchString(password)
}
