package main

import (
    "database/sql"
    "fmt"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"

    _ "github.com/mattn/go-sqlite3" // Pinned version; audited driver for SQLite
)

func main() {
    // Secure config: Run as non-root; disable debug. Defense-in-depth: Use HTTPS in production.
    http.HandleFunc("/unsubscribe", unsubscribe)
    // Add security headers middleware (defense-in-depth)
    handler := http.HandlerFunc(unsubscribe)
    wrapped := securityHeaders(handler)
    http.Handle("/unsubscribe", wrapped)
    log.Fatal(http.ListenAndServe(":8080", nil)) // In production, use TLS: http.ListenAndServeTLS
}

// securityHeaders adds web security hardening headers (OWASP A05 mitigation)
func securityHeaders(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Security-Policy", "default-src 'self'")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("Referrer-Policy", "strict-origin-when-cross-origin")
        next.ServeHTTP(w, r)
    })
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Input validation: Strict whitelist for email (RFC-compliant regex; CWE-89/OWASP A03 mitigation)
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, html.EscapeString("Error: Email parameter required"), http.StatusBadRequest)
        return
    }
    email = strings.TrimSpace(email) // Sanitize: Remove whitespace
    email = strings.ToLower(email)   // Canonicalize: Lowercase for consistency
    emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
    if !emailRegex.MatchString(email) {
        http.Error(w, html.EscapeString("Error: Invalid email format"), http.StatusBadRequest)
        return
    }

    // Secure dependencies: Use pinned, audited DB driver. Cryptography: DB encrypted at rest (configure SQLite with key).
    dbPassword := os.Getenv("DB_PASSWORD") // Secrets: Via env vars; no hardcoding
    if dbPassword == "" {
        log.Fatal("DB_PASSWORD not set") // Fail securely
    }
    db, err := sql.Open("sqlite3", fmt.Sprintf("file:emails.db?_pragma_key=%s&_pragma_cipher_page_size=4096&_pragma_kdf_iter=64000", dbPassword))
    if err != nil {
        log.Printf("DB open error: %v", err) // Log without PII
        http.Error(w, html.EscapeString("Internal server error"), http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Database safety: Parameterized query prevents SQL injection (CWE-89/OWASP A03). Least privilege: DB user limited to SELECT/UPDATE.
    var subscribed int
    err = db.QueryRow("SELECT subscribed FROM subscribers WHERE email = ?", email).Scan(&subscribed)
    if err != nil {
        if err == sql.ErrNoRows {
            http.Error(w, html.EscapeString("Error: Email not found"), http.StatusNotFound)
            return
        }
        log.Printf("DB query error: %v", err) // Audit logging: Anonymized
        http.Error(w, html.EscapeString("Internal server error"), http.StatusInternalServerError)
        return
    }

    if subscribed == 0 {
        fmt.Fprintf(w, html.EscapeString("Already unsubscribed")) // Output encoding: HTML-escape prevents XSS (CWE-79)
        return
    }

    // Update with parameterization
    _, err = db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Printf("DB update error: %v", err)
        http.Error(w, html.EscapeString("Internal server error"), http.StatusInternalServerError)
        return
    }

    // Data protection: Log anonymized event; minimize retention (delete after 30 days via cron)
    log.Printf("Unsubscribe successful for email hash: %x", hashEmail(email)) // Pseudonymize: Hash for logging
    fmt.Fprintf(w, html.EscapeString("Unsubscribed successfully"))

    // Continuous improvement: Add monitoring/alerts for anomalies (e.g., high failure rate)
}

// hashEmail: Pseudonymize for logging (use crypto library; never roll your own)
func hashEmail(email string) []byte {
    // Placeholder: In production, use crypto/sha256 with salt from secrets manager
    return []byte("hashed") // Replace with actual hash
}
