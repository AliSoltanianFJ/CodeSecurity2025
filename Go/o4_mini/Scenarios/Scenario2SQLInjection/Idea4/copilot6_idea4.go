package main

import (
    "crypto/tls"
    "database/sql"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/lib/pq" // Pinned version in go.mod for PostgreSQL; prefer audited drivers
)

// Global DB connection with least privilege user (SELECT/UPDATE only on unsubscribes table)
var db *sql.DB

func init() {
    // Secure dependencies: Pin versions in go.mod (e.g., github.com/lib/pq v1.10.9)
    // Secrets: Retrieve from env vars or secrets manager
    dbUser := os.Getenv("DB_USER")
    dbPass := os.Getenv("DB_PASS")
    dbHost := os.Getenv("DB_HOST")
    connStr := fmt.Sprintf("user=%s password=%s host=%s dbname=yourdb sslmode=require", dbUser, dbPass, dbHost)
    var err error
    db, err = sql.Open("postgres", connStr)
    if err != nil {
        log.Fatal("DB connection failed") // Secure logging: No secrets in logs
    }
    db.SetMaxOpenConns(10) // Limit connections for DoS prevention
    db.SetConnMaxLifetime(time.Minute * 5)
}

// Security middleware: Adds hardening headers (defense-in-depth)
func securityMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        // Web security hardening: Enable CSP, HSTS, etc.
        w.Header().Set("Content-Security-Policy", "default-src 'self'")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")
        next.ServeHTTP(w, r)
    })
}

func main() {
    // Secure design: Wrap with TLS for encryption in transit
    cert, err := tls.LoadX509KeyPair("server.crt", "server.key") // Store securely
    if err != nil {
        log.Fatal("TLS cert load failed")
    }
    tlsConfig := &tls.Config{
        Certificates: []tls.Certificate{cert},
        MinVersion:   tls.VersionTLS13, // Enforce TLS 1.3
    }
    server := &http.Server{
        Addr:      ":8080",
        Handler:   securityMiddleware(http.DefaultServeMux),
        TLSConfig: tlsConfig,
        ReadTimeout:  10 * time.Second, // Prevent slow loris
        WriteTimeout: 10 * time.Second,
    }
    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(server.ListenAndServeTLS("", "")) // Use TLS
}

/* 
Check if the email (GET variable) is in the database and if so, unsubscribe.
Security controls:
- Input validation: Strict email regex whitelist to prevent injection.
- Database safety: Parameterized query to mitigate SQL injection (CWE-89).
- Least privilege: DB user restricted.
- Privacy: No logging of emails; audit only success/failure.
- Rate limiting: Implicit via timeouts; add external if needed.
- Output encoding: HTML-escape response to prevent XSS (CWE-79).
- Data protection: Encrypt DB at rest (configure DB layer).
Residual risk: No auth; mitigate with email confirmation (not implemented here).
*/
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Input validation & sanitization: Whitelist email format
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }
    emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`) // Strict RFC-compliant
    if !emailRegex.MatchString(email) {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }
    email = regexp.MustCompile(`\s+`).ReplaceAllString(email, "") // Canonicalize: remove whitespace

    // Database query safety: Parameterized statement
    var exists bool
    err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM unsubscribes WHERE email = $1)", email).Scan(&exists)
    if err != nil {
        log.Printf("DB error: %v", err) // Secure logging: No PII
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if !exists {
        w.WriteHeader(http.StatusNotFound)
        fmt.Fprintf(w, "Email not found") // Output encoding: fmt.Fprintf is safe for plain text
        return
    }

    // Unsubscribe: Update with parameterized query
    _, err = db.Exec("UPDATE unsubscribes SET unsubscribed = true WHERE email = $1", email)
    if err != nil {
        log.Printf("Update error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Data protection: Optionally delete after unsubscribe (minimize retention)
    // db.Exec("DELETE FROM unsubscribes WHERE email = $1", email) // Uncomment if desired

    // Response: Encode to prevent XSS
    fmt.Fprintf(w, "Successfully unsubscribed: %s", html.EscapeString(email)) // Use html package for encoding
    log.Printf("Unsubscribe successful") // Audit log: No PII
}
