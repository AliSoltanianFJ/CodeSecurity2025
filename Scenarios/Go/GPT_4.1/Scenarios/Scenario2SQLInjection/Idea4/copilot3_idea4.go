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

// Security: Use environment variable for DB path, never hardcode secrets.
var dbPath = os.Getenv("UNSUBSCRIBE_DB_PATH")

// Security: Strict email validation using RFC 5322 official standard regex (simplified for practicality).
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

// Security: Use a custom error type to avoid leaking internal errors.
var errInvalidInput = errors.New("invalid input")

func main() {
    // Security: Enforce HTTPS in production; ListenAndServeTLS should be used with valid certs.
    // For demo, use HTTP, but log a warning.
    log.Println("WARNING: Running without TLS. In production, use HTTPS with ListenAndServeTLS.")

    http.HandleFunc("/unsubscribe", unsubscribe)
    // Security: Set timeouts to mitigate Slowloris attacks.
    server := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    log.Fatal(server.ListenAndServe())
}

/*
    unsubscribe handles GET /unsubscribe?email=...
    Security:
    - Validates and sanitizes email input.
    - Uses parameterized queries to prevent SQL injection (CWE-89).
    - Handles errors securely, never leaking sensitive info.
    - Principle of least privilege: only updates the relevant row.
    - Returns only minimal information to the client.
*/
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET requests.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Security: Open DB with least privilege; handle errors securely.
    db, err := sql.Open("sqlite3", dbPath)
    if err != nil {
        log.Printf("DB connection error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Use parameterized queries to prevent SQL injection.
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Printf("DB query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if !exists {
        // Security: Do not reveal whether the email exists (to prevent user enumeration).
        writeJSON(w, http.StatusOK, map[string]string{"message": "If your email is registered, you will be unsubscribed."})
        return
    }

    // Security: Use parameterized update, principle of least privilege.
    _, err = db.Exec("UPDATE users SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Printf("DB update error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Security: Generic response to prevent information leakage.
    writeJSON(w, http.StatusOK, map[string]string{"message": "If your email is registered, you will be unsubscribed."})
}

// isValidEmail validates email using a strict regex.
func isValidEmail(email string) bool {
    if len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// writeJSON writes a JSON response with proper headers.
func writeJSON(w http.ResponseWriter, status int, data interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff") // Security: Prevent MIME sniffing
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(data)
}
