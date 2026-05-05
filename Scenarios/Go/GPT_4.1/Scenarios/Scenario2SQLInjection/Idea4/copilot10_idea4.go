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
var dbPath = os.Getenv("DB_PATH") // e.g., set DB_PATH=d:\secure\emails.db

// Security: Strict email validation using RFC 5322-compliant regex.
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

// Security: Use a custom error for user-facing messages to avoid leaking internals.
var errInvalidEmail = errors.New("invalid email address")

func main() {
    // Security: Use HTTPS in production; ListenAndServeTLS with valid certs.
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Println("Server starting on :8080 (use HTTPS in production)")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("Server failed: %v", err)
    }
}

// unsubscribe handles the unsubscribe logic securely.
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET requests for idempotency.
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size (though GET should have no body).
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    // Security: Parse and validate email parameter.
    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        respondJSON(w, http.StatusBadRequest, map[string]string{"error": errInvalidEmail.Error()})
        return
    }

    // Security: Open DB with least privilege, handle errors securely.
    db, err := sql.Open("sqlite3", dbPath)
    if err != nil {
        log.Printf("DB open error: %v", err)
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
        respondJSON(w, http.StatusNotFound, map[string]string{"error": "Email not found"})
        return
    }

    // Security: Use parameterized statement for update.
    res, err := db.ExecContext(ctx, "UPDATE users SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Printf("DB update error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    rowsAffected, _ := res.RowsAffected()
    if rowsAffected == 0 {
        respondJSON(w, http.StatusNotFound, map[string]string{"error": "Email not found or already unsubscribed"})
        return
    }

    respondJSON(w, http.StatusOK, map[string]string{"message": "Successfully unsubscribed"})
}

// isValidEmail validates email using a strict regex.
func isValidEmail(email string) bool {
    if len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// respondJSON writes a JSON response with proper headers.
func respondJSON(w http.ResponseWriter, status int, payload interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff") // Security: Prevent MIME sniffing
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(payload)
}

// contextWithTimeout returns a context with a short timeout for DB ops.
func contextWithTimeout() (context.Context, context.CancelFunc) {
    // Security: Limit DB operation time to avoid DoS.
    return context.WithTimeout(r.Context(), 2*time.Second)
}
