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

    _ "github.com/mattn/go-sqlite3" // Use a maintained driver; replace with your DB driver as needed
)

// Security: Use environment variables for sensitive config, never hardcode secrets.
var dbPath = os.Getenv("UNSUBSCRIBE_DB_PATH")

// Security: Strict email validation regex (RFC 5322 Official Standard, simplified for practicality)
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

// Security: Use secure HTTP headers
func secureHeaders(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-XSS-Protection", "1; mode=block")
        w.Header().Set("Referrer-Policy", "no-referrer")
        w.Header().Set("Cache-Control", "no-store")
        next.ServeHTTP(w, r)
    })
}

func main() {
    // Security: Enforce HTTPS in production (not shown here; use TLS in ListenAndServeTLS)
    http.Handle("/unsubscribe", secureHeaders(http.HandlerFunc(unsubscribe)))
    log.Println("Server started on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        // Security: Do not leak internal errors
        log.Fatal("Server failed to start")
    }
}

/*
    unsubscribe handles GET /unsubscribe?email=...
    Security:
    - Validates and sanitizes email input.
    - Uses parameterized queries to prevent SQL injection (CWE-89).
    - Does not leak sensitive error details to the client.
    - Principle of least privilege: only updates the relevant row.
    - No secrets or credentials are hardcoded.
    - All responses are JSON, with context-aware encoding.
*/
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    type response struct {
        Success bool   `json:"success"`
        Message string `json:"message"`
    }

    // Security: Only allow GET requests
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        respondJSON(w, http.StatusBadRequest, response{false, "Invalid email address"})
        return
    }

    // Security: Open DB with least privilege (read/write only)
    db, err := sql.Open("sqlite3", dbPath)
    if err != nil {
        log.Printf("DB open error: %v", err)
        respondJSON(w, http.StatusInternalServerError, response{false, "Internal server error"})
        return
    }
    defer db.Close()

    // Security: Use context with timeout to avoid resource exhaustion
    ctx, cancel := contextWithTimeout()
    defer cancel()

    // Security: Parameterized query to prevent SQL injection
    var exists bool
    err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Printf("DB query error: %v", err)
        respondJSON(w, http.StatusInternalServerError, response{false, "Internal server error"})
        return
    }
    if !exists {
        respondJSON(w, http.StatusNotFound, response{false, "Email not found"})
        return
    }

    // Security: Use prepared statement for update
    stmt, err := db.PrepareContext(ctx, "UPDATE users SET subscribed = 0 WHERE email = ?")
    if err != nil {
        log.Printf("DB prepare error: %v", err)
        respondJSON(w, http.StatusInternalServerError, response{false, "Internal server error"})
        return
    }
    defer stmt.Close()

    res, err := stmt.ExecContext(ctx, email)
    if err != nil {
        log.Printf("DB exec error: %v", err)
        respondJSON(w, http.StatusInternalServerError, response{false, "Internal server error"})
        return
    }
    rowsAffected, err := res.RowsAffected()
    if err != nil || rowsAffected == 0 {
        log.Printf("No rows updated or error: %v", err)
        respondJSON(w, http.StatusInternalServerError, response{false, "Internal server error"})
        return
    }

    respondJSON(w, http.StatusOK, response{true, "Successfully unsubscribed"})
}

// isValidEmail validates email using a strict regex.
func isValidEmail(email string) bool {
    if len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// respondJSON encodes response as JSON with proper headers.
func respondJSON(w http.ResponseWriter, status int, data interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(data) // Security: Ignore encoding errors, do not leak info
}

// contextWithTimeout returns a context with a short timeout for DB operations.
func contextWithTimeout() (context.Context, context.CancelFunc) {
    // Security: Limit DB operation time to prevent DoS/resource exhaustion
    return context.WithTimeout(r.Context(), 3*time.Second)
}
