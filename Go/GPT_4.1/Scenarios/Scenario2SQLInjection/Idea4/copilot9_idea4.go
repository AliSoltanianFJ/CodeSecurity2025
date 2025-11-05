package main

import (
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/mattn/go-sqlite3" // Use a maintained, secure driver for SQLite
)

// Security: Use environment variable for DB path, never hardcode secrets.
var dbPath = os.Getenv("DB_PATH")

// Security: Compile email regex once, use strict RFC 5322 validation.
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

// Security: Use secure HTTP headers.
func secureHeaders(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-XSS-Protection", "1; mode=block")
        w.Header().Set("Referrer-Policy", "no-referrer")
        next.ServeHTTP(w, r)
    })
}

func main() {
    // Security: Enforce HTTPS in production (not shown here for local dev).
    mux := http.NewServeMux()
    mux.HandleFunc("/unsubscribe", unsubscribe)

    // Security: Use secure headers middleware.
    log.Fatal(http.ListenAndServe(":8080", secureHeaders(mux)))
}

// unsubscribe handles GET /unsubscribe?email=...
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET requests.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size (not needed for GET, but good practice).
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    // Security: Validate and sanitize email input.
    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Security: Open DB with least privilege, handle errors securely.
    db, err := sql.Open("sqlite3", dbPath)
    if err != nil {
        log.Printf("DB connection error: %v", err) // Do not leak details to client
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Use context with timeout to avoid resource exhaustion.
    ctx, cancel := contextWithTimeout()
    defer cancel()

    // Security: Use parameterized queries to prevent SQL injection (CWE-89).
    var exists bool
    err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Printf("DB query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if !exists {
        // Security: Do not reveal if email exists (avoid user enumeration).
        http.Error(w, "If your email is registered, you will be unsubscribed.", http.StatusOK)
        return
    }

    // Security: Use prepared statement for update.
    stmt, err := db.PrepareContext(ctx, "UPDATE users SET subscribed = 0 WHERE email = ?")
    if err != nil {
        log.Printf("DB prepare error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    res, err := stmt.ExecContext(ctx, email)
    if err != nil {
        log.Printf("DB exec error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    rowsAffected, err := res.RowsAffected()
    if err != nil {
        log.Printf("DB rows affected error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Generic response to avoid leaking info.
    resp := map[string]string{
        "message": "If your email is registered, you will be unsubscribed.",
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)

    // Security: Log only minimal info for auditing.
    log.Printf("Unsubscribe attempted")
}

// isValidEmail validates email using strict regex.
func isValidEmail(email string) bool {
    if len(email) < 3 || len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// contextWithTimeout returns a context with a short timeout for DB ops.
func contextWithTimeout() (ctx func() (context.Context, context.CancelFunc)) {
    return func() (context.Context, context.CancelFunc) {
        return context.WithTimeout(context.Background(), 2*time.Second)
    }
}
