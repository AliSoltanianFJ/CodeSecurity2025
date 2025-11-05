package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

// Security decision: Do not hardcode credentials. Use environment variables or a secure secrets manager.
func getDBConnStr() (string, error) {
    user := os.Getenv("DB_USER")
    pass := os.Getenv("DB_PASS")
    host := os.Getenv("DB_HOST")
    port := os.Getenv("DB_PORT")
    name := os.Getenv("DB_NAME")
    if user == "" || pass == "" || host == "" || port == "" || name == "" {
        return "", errors.New("database credentials not set in environment variables")
    }
    // Principle of least privilege: Use a DB user with minimal permissions.
    return user + ":" + pass + "@tcp(" + host + ":" + port + ")/" + name + "?parseTime=true&tls=true", nil
}

// Security: Limit request body size to prevent DoS attacks.
const maxRequestBodySize = 1024 // 1KB

// Security: Use a struct for input validation.
type loginRequest struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

// Security: Use a struct for output to avoid leaking sensitive info.
type loginResponse struct {
    Message string `json:"message"`
}

func main() {
    http.HandleFunc("/login", loginHandler)
    // Security: Use HTTPS in production to protect data in transit.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size.
    r.Body = http.MaxBytesReader(w, r.Body, maxRequestBodySize)
    defer r.Body.Close()

    var req loginRequest
    decoder := json.NewDecoder(r.Body)
    if err := decoder.Decode(&req); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    // Security: Validate and sanitize input.
    req.Username = strings.TrimSpace(req.Username)
    req.Password = strings.TrimSpace(req.Password)
    if !isValidUsername(req.Username) || !isValidPassword(req.Password) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    connStr, err := getDBConnStr()
    if err != nil {
        // Security: Do not leak sensitive error details.
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB connection error: %v", err)
        return
    }

    db, err := sql.Open("mysql", connStr)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB open error: %v", err)
        return
    }
    defer db.Close()

    // Security: Use context with timeout to avoid resource exhaustion.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Security: Use parameterized queries to prevent SQL injection.
    var hashedPassword string
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", req.Username).Scan(&hashedPassword)
    if err != nil {
        // Do not reveal if username exists.
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security: Use constant-time comparison for password hashes.
    if bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(req.Password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security: On success, do not return sensitive info.
    resp := loginResponse{Message: "Login successful"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// Security: Validate username (e.g., alphanumeric, reasonable length).
func isValidUsername(username string) bool {
    if len(username) < 3 || len(username) > 32 {
        return false
    }
    for _, c := range username {
        if !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
            return false
        }
    }
    return true
}

// Security: Validate password (length, no whitespace).
func isValidPassword(password string) bool {
    if len(password) < 8 || len(password) > 64 {
        return false
    }
    if strings.ContainsAny(password, " \t\n\r") {
        return false
    }
    return true
}
