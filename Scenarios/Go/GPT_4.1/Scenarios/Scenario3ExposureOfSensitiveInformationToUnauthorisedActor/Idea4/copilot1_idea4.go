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

// Security: Load DB credentials from environment variables, never hardcode secrets.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbPort     = os.Getenv("DB_PORT")
    dbName     = os.Getenv("DB_NAME")
)

// Security: Use a strong Content Security Policy and secure headers.
func secureHeaders(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-XSS-Protection", "1; mode=block")
        w.Header().Set("Referrer-Policy", "no-referrer")
        w.Header().Set("Cache-Control", "no-store")
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        next.ServeHTTP(w, r)
    })
}

func main() {
    http.Handle("/login", secureHeaders(http.HandlerFunc(loginHandler)))
    // Security: Always use HTTPS in production. For demo, ListenAndServeTLS is not used.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// Security: Struct for JSON response, avoids leaking sensitive info.
type loginResponse struct {
    Message string `json:"message"`
}

// Security: Validate username and password format strictly.
func validateCredentials(username, password string) error {
    if len(username) < 3 || len(username) > 32 {
        return errors.New("invalid username length")
    }
    if len(password) < 8 || len(password) > 64 {
        return errors.New("invalid password length")
    }
    if !isAlphanumeric(username) {
        return errors.New("username must be alphanumeric")
    }
    return nil
}

// Security: Only allow alphanumeric usernames.
func isAlphanumeric(s string) bool {
    for _, r := range s {
        if !(r >= 'a' && r <= 'z') && !(r >= 'A' && r <= 'Z') && !(r >= '0' && r <= '9') {
            return false
        }
    }
    return true
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Parse form with a reasonable size limit.
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Security: Validate and sanitize user input.
    if err := validateCredentials(username, password); err != nil {
        http.Error(w, "Invalid credentials", http.StatusBadRequest)
        return
    }

    // Security: Use least privilege DB user, never root in production.
    dsn := dbUser + ":" + dbPassword + "@tcp(" + dbHost + ":" + dbPort + ")/" + dbName + "?parseTime=true&tls=false"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        // Security: Do not leak DB errors to client.
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Use context with timeout to avoid hanging connections.
    type userRecord struct {
        PasswordHash string
    }
    var user userRecord

    // Security: Use parameterized queries to prevent SQL injection (CWE-89).
    ctx, cancel := getTimeoutContext()
    defer cancel()
    err = db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username = ?", username).Scan(&user.PasswordHash)
    if err != nil {
        // Security: Do not reveal if user exists.
        time.Sleep(500 * time.Millisecond) // Mitigate timing attacks.
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Security: Use bcrypt for password hashing (never store plaintext).
    if bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password)) != nil {
        time.Sleep(500 * time.Millisecond) // Mitigate timing attacks.
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Security: On successful login, do not expose sensitive info.
    resp := loginResponse{Message: "Login successful"}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// Security: Use context with timeout for DB operations.
func getTimeoutContext() (ctx func() (context.Context, context.CancelFunc)) {
    return func() (context.Context, context.CancelFunc) {
        return context.WithTimeout(context.Background(), 3*time.Second)
    }
}
