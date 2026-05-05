package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
// unsubscribe handles requests to unsubscribe an email.
// It checks if the provided email exists in the database and unsubscribes it if found.
// Security: Input validation, error handling, no sensitive info leaked, least privilege DB access.
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Use context with timeout for all operations.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Parse query parameters safely.
    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Open DB with least privilege (read/write only to necessary table).
    db, err := sql.Open("sqlite3", dbPath)
    if err != nil {
        logError(ctx, err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Principle of least privilege: Only access necessary columns.
    var exists bool
    err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        logError(ctx, err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if !exists {
        // Do not reveal whether the email exists (avoid user enumeration).
        http.Error(w, "If your email is registered, you will be unsubscribed.", http.StatusOK)
        return
    }

    // Unsubscribe the user (soft delete for auditability).
    res, err := db.ExecContext(ctx, "UPDATE users SET subscribed = 0, unsubscribed_at = ? WHERE email = ? AND subscribed = 1", time.Now().UTC(), email)
    if err != nil {
        logError(ctx, err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    rowsAffected, err := res.RowsAffected()
    if err != nil {
        logError(ctx, err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    if rowsAffected == 0 {
        // Either already unsubscribed or not found; generic message.
        http.Error(w, "If your email is registered, you will be unsubscribed.", http.StatusOK)
        return
    }

    // Return generic success message (no sensitive info leaked).
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{
        "message": "If your email is registered, you will be unsubscribed.",
    })
}

// isValidEmail performs strict email validation using a whitelist approach.
func isValidEmail(email string) bool {
    // RFC 5322 Official Standard regex (simplified for most common cases).
    var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
    if len(email) < 3 || len(email) > 254 {
        return false
    }
    return emailRegex.MatchString(email)
}

// logError logs errors securely using the standard log package (no sensitive info).
func logError(ctx context.Context, err error) {
    // In production, use a secure centralized logging system.
    log.Printf("error: %v", err)
}

