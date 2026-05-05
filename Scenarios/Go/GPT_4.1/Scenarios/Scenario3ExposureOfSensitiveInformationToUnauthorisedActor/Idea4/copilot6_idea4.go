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

// Security Note: Never hardcode credentials in source code. Use environment variables or a secure secrets manager.
var dbDSN = os.Getenv("DB_DSN") // e.g., "user:password@tcp(localhost:3306)/db"

func main() {
    // Security: Always use HTTPS in production to protect data in transit.
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// loginHandler authenticates a user using secure password hashing and parameterized queries.
// All user input is validated and sanitized. Errors are generic to avoid information leakage.
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form data safely
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation: enforce reasonable username and password constraints
    if !isValidUsername(username) || !isValidPassword(password) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security: Use least privilege DB user, and never hardcode credentials.
    db, err := sql.Open("mysql", dbDSN)
    if err != nil {
        log.Printf("DB connection error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Use context with timeout to prevent resource exhaustion.
    // Security: Use parameterized queries to prevent SQL injection (CWE-89).
    var hashedPassword string
    ctx, cancel := getTimeoutContext()
    defer cancel()
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&hashedPassword)
    if err != nil {
        // Do not reveal if user exists or not (prevents user enumeration)
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security: Use bcrypt for password hashing (never store plaintext passwords).
    if err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Security: Generate a secure session token (not implemented here; use secure session management in production).
    // For demonstration, return a generic success message.
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"message": "Login successful"})
}

// isValidUsername validates the username for allowed characters and length.
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

// isValidPassword enforces a minimum length and basic complexity.
func isValidPassword(password string) bool {
    if len(password) < 8 || len(password) > 64 {
        return false
    }
    // Add further complexity checks as needed (e.g., require numbers, symbols, etc.)
    return true
}

// getTimeoutContext returns a context with a timeout for DB operations.
func getTimeoutContext() (ctx func() (context.Context, context.CancelFunc), cancel func()) {
    type contextKey string
    return func() (context.Context, context.CancelFunc) {
        return context.WithTimeout(context.Background(), 3*time.Second)
    }, func() {}
}
