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

// Security: Use environment variables for sensitive configuration, never hardcode secrets.
var dbPath = os.Getenv("DB_PATH") // e.g., set DB_PATH=d:\secure\app.db

// Security: Strict email validation using RFC 5322 official standard regex (simplified for practicality).
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

// Security: Use least privilege, open DB with minimal permissions.
func openDB() (*sql.DB, error) {
    if dbPath == "" {
        return nil, errors.New("database path not set")
    }
    db, err := sql.Open("sqlite3", dbPath+"?_busy_timeout=5000&_journal_mode=WAL&_foreign_keys=ON")
    if err != nil {
        return nil, err
    }
    // Security: Set connection limits to mitigate DoS.
    db.SetMaxOpenConns(5)
    db.SetMaxIdleConns(2)
    db.SetConnMaxLifetime(5 * time.Minute)
    return db, nil
}

func main() {
    // Security: Enforce HTTPS in production (not shown here for local dev).
    http.HandleFunc("/unsubscribe", unsubscribe)
    // Security: Use a secure server configuration in production.
    log.Fatal(http.ListenAndServe(":8080", nil))
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

    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    db, err := openDB()
    if err != nil {
        // Security: Do not leak internal errors.
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB open error: %v", err)
        return
    }
    defer db.Close()

    // Security: Use parameterized queries to prevent SQL injection (CWE-89).
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }

    if !exists {
        // Security: Do not reveal whether an email exists (to prevent user enumeration).
        writeJSON(w, http.StatusOK, map[string]string{"message": "If your email is registered, you will be unsubscribed."})
        return
    }

    // Security: Use parameterized statement for update.
    res, err := db.Exec("UPDATE users SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("DB update error: %v", err)
        return
    }
    rowsAffected, _ := res.RowsAffected()
    if rowsAffected == 0 {
        // Security: Same generic message to prevent enumeration.
        writeJSON(w, http.StatusOK, map[string]string{"message": "If your email is registered, you will be unsubscribed."})
        return
    }

    writeJSON(w, http.StatusOK, map[string]string{"message": "If your email is registered, you will be unsubscribed."})
}

// isValidEmail validates email using a strict regex.
func isValidEmail(email string) bool {
    if len(email) < 3 || len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// writeJSON writes a JSON response with proper headers.
func writeJSON(w http.ResponseWriter, status int, data interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff") // Security: Prevent MIME sniffing
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // Security: Prevent XSS in JSON
    _ = enc.Encode(data)
}
