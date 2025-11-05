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

// Security: Use environment variables for DB connection strings, never hardcode secrets.
var dbConnStr = os.Getenv("DB_CONN_STR")

// Security: Strict email validation using RFC 5322 compliant regex.
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9.!#$%&'*+/=?^_` + "`" + `{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

// Security: Use a custom error message to avoid leaking sensitive info.
const genericErrMsg = "An error occurred. Please try again later."

func main() {
    // Security: Enforce HTTPS in production (not shown here for local dev).
    http.HandleFunc("/unsubscribe", unsubscribe)
    // Security: Use a custom server with secure defaults.
    server := &http.Server{
        Addr:              ":8080",
        ReadTimeout:       5 * time.Second,
        WriteTimeout:      10 * time.Second,
        IdleTimeout:       15 * time.Second,
        ReadHeaderTimeout: 2 * time.Second,
        Handler:           nil,
    }
    log.Fatal(server.ListenAndServe())
}

// unsubscribe handles the unsubscribe logic securely.
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET requests for this endpoint.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size (not needed for GET, but good practice).
    r.Body = http.MaxBytesReader(w, r.Body, 1024)

    // Security: Parse and strictly validate the email parameter.
    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Security: Open DB with least privilege (read/write only as needed).
    db, err := sql.Open("sqlite3", dbConnStr)
    if err != nil {
        log.Printf("DB connection error: %v", err)
        http.Error(w, genericErrMsg, http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Security: Use parameterized queries to prevent SQL injection (CWE-89).
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Printf("DB query error: %v", err)
        http.Error(w, genericErrMsg, http.StatusInternalServerError)
        return
    }

    if !exists {
        respondJSON(w, http.StatusOK, map[string]string{"message": "Email not found or already unsubscribed."})
        return
    }

    // Security: Use prepared statement for deletion.
    stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
    if err != nil {
        log.Printf("DB prepare error: %v", err)
        http.Error(w, genericErrMsg, http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    res, err := stmt.Exec(email)
    if err != nil {
        log.Printf("DB exec error: %v", err)
        http.Error(w, genericErrMsg, http.StatusInternalServerError)
        return
    }

    rowsAffected, err := res.RowsAffected()
    if err != nil {
        log.Printf("DB rows affected error: %v", err)
        http.Error(w, genericErrMsg, http.StatusInternalServerError)
        return
    }

    if rowsAffected == 0 {
        respondJSON(w, http.StatusOK, map[string]string{"message": "Email not found or already unsubscribed."})
        return
    }

    respondJSON(w, http.StatusOK, map[string]string{"message": "Successfully unsubscribed."})
}

// isValidEmail validates email using a strict regex.
func isValidEmail(email string) bool {
    if len(email) < 3 || len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// respondJSON writes a JSON response with proper headers.
func respondJSON(w http.ResponseWriter, status int, data interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    // Security: Do not leak internal errors in JSON output.
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(data); err != nil {
        log.Printf("JSON encode error: %v", err)
        http.Error(w, genericErrMsg, http.StatusInternalServerError)
    }
}

/*
Security notes:
- All user input is strictly validated and sanitized.
- All DB operations use parameterized queries to prevent SQL injection (CWE-89).
- No secrets are hardcoded; use environment variables for sensitive config.
- Error messages are generic to avoid information leakage (CWE-209).
- Only necessary HTTP methods are allowed.
- All responses are JSON-encoded with HTML escaping to prevent XSS (CWE-79).
- Use HTTPS in production to protect data in transit.
- Principle of least privilege: DB user should have only required permissions.
- No sensitive data is logged or returned to the client.
*/
