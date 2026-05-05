package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"

    _ "github.com/lib/pq" // PostgreSQL driver; pin version in go.mod for security
)

func main() {
    // Secure config: Enforce HTTPS in production (redirect HTTP to HTTPS)
    // Defense-in-depth: Run as non-root, use AppArmor/seccomp
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServeTLS(":8080", "server.crt", "server.key", nil)) // Use TLS 1.3
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Web hardening: Set security headers
    w.Header().Set("Content-Security-Policy", "default-src 'self'")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")
    w.Header().Set("Strict-Transport-Security", "max-age=31536000; includeSubDomains")

    // Input validation: Strict whitelist for email format (RFC 5322 simplified)
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Email parameter required", http.StatusBadRequest)
        return
    }
    email = strings.ToLower(strings.TrimSpace(email)) // Canonicalize
    emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
    if !emailRegex.MatchString(email) {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }

    // Database safety: Use parameterized query to prevent SQL injection (CWE-89)
    // Least privilege: DB user has only SELECT/DELETE on subscriptions table
    db, err := sql.Open("postgres", os.Getenv("DB_CONN_STRING")) // Secrets via env vars
    if err != nil {
        log.Printf("DB connection error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Check if email exists
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE email = $1)", email).Scan(&exists)
    if err != nil {
        log.Printf("Query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if !exists {
        // Output encoding: HTML-encode to prevent XSS (CWE-79)
        fmt.Fprintf(w, "Email %s not found in subscriptions.", html.EscapeString(email))
        return
    }

    // Unsubscribe: Delete record
    _, err = db.Exec("DELETE FROM subscriptions WHERE email = $1", email)
    if err != nil {
        log.Printf("Delete error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Data protection: Log without PII (hash for auditing)
    log.Printf("Unsubscribed: %x", sha256.Sum256([]byte(email)))
    fmt.Fprintf(w, "Successfully unsubscribed %s.", html.EscapeString(email))
}
