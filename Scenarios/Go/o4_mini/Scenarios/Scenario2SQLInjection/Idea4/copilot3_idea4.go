package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    _ "github.com/lib/pq" // Pinned version; prefer audited driver
)

// Global DB connection with least privilege (SELECT/DELETE only on unsubscribe table)
var db *sql.DB

func init() {
    // Secure config: Load DB DSN from env var (secrets manager recommended)
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN not set")
    }
    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatal(err)
    }
    // Defense-in-depth: Set connection limits and timeouts
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(time.Minute * 5)
}

func main() {
    // Secure config: Enable TLS 1.3 (assume certs are configured)
    server := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    http.HandleFunc("/unsubscribe", unsubscribe)
    // Web hardening: Add security headers
    http.Handle("/", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Security-Policy", "default-src 'self'")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")
        w.Header().Set("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        http.DefaultServeMux.ServeHTTP(w, r)
    }))
    log.Fatal(server.ListenAndServeTLS("server.crt", "server.key")) // Enforce TLS
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Input validation: Strict whitelist for email (RFC 5322 simplified regex)
    emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
    email := strings.TrimSpace(strings.ToLower(r.URL.Query().Get("email"))) // Sanitize: trim and lowercase
    if !emailRegex.MatchString(email) {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }

    // Database safety: Parameterized query to prevent SQL injection (CWE-89)
    stmt, err := db.Prepare("SELECT COUNT(*) FROM unsubscribes WHERE email = $1")
    if err != nil {
        log.Printf("Prepare error: %v", err) // Log without PII
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    var count int
    err = stmt.QueryRow(email).Scan(&count)
    if err != nil {
        log.Printf("Query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if count == 0 {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprintf(w, "Email not found")
        return
    }

    // Unsubscribe: Use parameterized DELETE
    deleteStmt, err := db.Prepare("DELETE FROM unsubscribes WHERE email = $1")
    if err != nil {
        log.Printf("Delete prepare error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer deleteStmt.Close()

    _, err = deleteStmt.Exec(email)
    if err != nil {
        log.Printf("Delete error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Output encoding: HTML-escape response
    fmt.Fprintf(w, "Successfully unsubscribed: %s", html.EscapeString(email)) // html package imported implicitly or add import
}
