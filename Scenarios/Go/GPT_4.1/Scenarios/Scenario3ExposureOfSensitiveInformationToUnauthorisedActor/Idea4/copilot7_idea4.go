package main

import (
    "crypto/subtle"
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

// Security decision: Do not hardcode secrets. Use environment variables or secure vaults.
var (
    dbUser     = os.Getenv("DB_USER")
    dbPassword = os.Getenv("DB_PASSWORD")
    dbHost     = os.Getenv("DB_HOST")
    dbPort     = os.Getenv("DB_PORT")
    dbName     = os.Getenv("DB_NAME")
)

// Security decision: Use a strong Content Security Policy and secure headers.
func secureHeaders(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-XSS-Protection", "1; mode=block")
        w.Header().Set("Referrer-Policy", "no-referrer")
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
        next.ServeHTTP(w, r)
    })
}

func main() {
    // Security decision: Only listen on localhost for development. Use HTTPS in production.
    mux := http.NewServeMux()
    mux.HandleFunc("/login", loginHandler)
    log.Println("Server starting on 127.0.0.1:8080")
    err := http.ListenAndServe("127.0.0.1:8080", secureHeaders(mux))
    if err != nil {
        log.Fatalf("Server failed to start: %v", err)
    }
}

// Security decision: Do not leak error details to the client. Log securely.
func respondWithError(w http.ResponseWriter, code int, message string) {
    http.Error(w, http.StatusText(code), code)
    log.Printf("Error: %s", message)
}

// Security decision: Validate and sanitize all user input.
func sanitizeInput(input string) (string, error) {
    trimmed := strings.TrimSpace(input)
    if len(trimmed) < 3 || len(trimmed) > 64 {
        return "", errors.New("input length invalid")
    }
    if strings.ContainsAny(trimmed, "'\";--") {
        return "", errors.New("input contains invalid characters")
    }
    return trimmed, nil
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        respondWithError(w, http.StatusMethodNotAllowed, "Only POST allowed")
        return
    }

    // Security decision: Enforce application/json content type.
    if r.Header.Get("Content-Type") != "application/json" {
        respondWithError(w, http.StatusUnsupportedMediaType, "Content-Type must be application/json")
        return
    }

    type loginRequest struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    var req loginRequest
    decoder := json.NewDecoder(r.Body)
    decoder.DisallowUnknownFields()
    if err := decoder.Decode(&req); err != nil {
        respondWithError(w, http.StatusBadRequest, "Invalid JSON")
        return
    }

    username, err := sanitizeInput(req.Username)
    if err != nil {
        respondWithError(w, http.StatusBadRequest, "Invalid username")
        return
    }
    password := req.Password
    if len(password) < 8 || len(password) > 128 {
        respondWithError(w, http.StatusBadRequest, "Invalid password length")
        return
    }

    // Security decision: Use least privilege DB user, do not hardcode credentials.
    dsn := dbUser + ":" + dbPassword + "@tcp(" + dbHost + ":" + dbPort + ")/" + dbName + "?parseTime=true&tls=false"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        respondWithError(w, http.StatusInternalServerError, "Database connection failed")
        return
    }
    defer db.Close()

    // Security decision: Use context with timeout for DB operations.
    type userRecord struct {
        ID           int
        Username     string
        PasswordHash string
    }
    var user userRecord

    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Security decision: Use parameterized queries to prevent SQL injection (CWE-89).
    stmt := `SELECT id, username, password_hash FROM users WHERE username = ? LIMIT 1`
    err = db.QueryRowContext(ctx, stmt, username).Scan(&user.ID, &user.Username, &user.PasswordHash)
    if err == sql.ErrNoRows {
        // Security decision: Do not reveal whether username or password is incorrect.
        respondWithError(w, http.StatusUnauthorized, "Invalid credentials")
        return
    } else if err != nil {
        respondWithError(w, http.StatusInternalServerError, "Database query failed")
        return
    }

    // Security decision: Use constant time comparison to prevent timing attacks (CWE-208).
    err = bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password))
    if err != nil {
        respondWithError(w, http.StatusUnauthorized, "Invalid credentials")
        return
    }

    // Security decision: Generate a secure session token (not implemented here).
    // Set secure, HttpOnly, SameSite cookie attributes.
    http.SetCookie(w, &http.Cookie{
        Name:     "session_token",
        Value:    "securely-generated-session-token", // Replace with real token generator.
        Path:     "/",
        HttpOnly: true,
        Secure:   true,
        SameSite: http.SameSiteStrictMode,
        MaxAge:   3600,
    })

    // Security decision: Do not return sensitive information in response.
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"status": "success"})
}
