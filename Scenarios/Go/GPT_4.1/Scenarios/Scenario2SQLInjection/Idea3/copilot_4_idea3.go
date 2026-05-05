package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Sanitize input: Only allow valid email format
    email := r.URL.Query().Get("email")
    if !isValidEmail(email) {
        http.Error(w, "Invalid email address", http.StatusBadRequest)
        return
    }

    // Use parameterized queries to prevent SQL injection
    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Println("DB open error:", err)
        return
    }
    defer db.Close()

    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        log.Println("DB query error:", err)
        return
    }

    if !exists {
        http.Error(w, "Email not found", http.StatusNotFound)
        return
    }

    // Unsubscribe (delete) using parameterized query
    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
        log.Println("DB delete error:", err)
        return
    }

    w.Write([]byte("Successfully unsubscribed"))
}

// Simple email validation using regex
func isValidEmail(email string) bool {
    re := regexp.MustCompile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
    return re.MatchString(email)
}