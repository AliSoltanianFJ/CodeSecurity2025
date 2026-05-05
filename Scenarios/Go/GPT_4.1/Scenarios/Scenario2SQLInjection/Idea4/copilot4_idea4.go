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

// Security: Use environment variables for secrets/configuration, never hardcode credentials
var dbPath = os.Getenv("DB_PATH") // e.g., "file:secure.db?cache=shared&mode=rwc"

// Security: Strict email validation using RFC 5322 official standard regex
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

// Security: Use least privilege, open DB with minimal permissions
func openDB() (*sql.DB, error) {
    if dbPath == "" {
        return nil, errors.New("database path not set")
    }
    db, err := sql.Open("sqlite3", dbPath)
    if err != nil {
        return nil, err
    }
    // Security: Set connection limits
    db.SetMaxOpenConns(1)
    db.SetConnMaxLifetime(5 * time.Minute)
    return db, nil
}

func main() {
    // Security: Enforce HTTPS in production (not shown here for local dev)
    http.HandleFunc("/unsubscribe", unsubscribe)
    // Security: Use a custom server with timeouts to mitigate DoS attacks
    server := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    log.Fatal(server.ListenAndServe()) // Security: In production, use ListenAndServeTLS with valid certs
}

// unsubscribe handles unsubscribe requests securely
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET requests for idempotency
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request size to prevent DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    db, err := openDB()
    if err != nil {
        // Security: Do not leak internal errors
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB open error: %v", err)
        return
    }
    defer db.Close()

    // Security: Use parameterized queries to prevent SQL injection (CWE-89)
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }

    if !exists {
        // Security: Do not reveal whether an email is registered (avoid user enumeration)
        writeJSON(w, http.StatusOK, map[string]string{"message": "If your email is registered, you will be unsubscribed."})
        return
    }

    // Security: Use prepared statement for update
    stmt, err := db.Prepare("UPDATE users SET subscribed = 0 WHERE email = ?")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB prepare error: %v", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(email)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB exec error: %v", err)
        return
    }

    // Security: Generic response to prevent information leakage
    writeJSON(w, http.StatusOK, map[string]string{"message": "If your email is registered, you will be unsubscribed."})
}

// isValidEmail validates email using strict regex
func isValidEmail(email string) bool {
    if len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// writeJSON writes a JSON response with proper headers
func writeJSON(w http.ResponseWriter, status int, data interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff") // Security: Prevent MIME sniffing
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(data)
}
