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
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Email parameter is required.", http.StatusBadRequest)
        return
    }

    // Simple email format validation
    if !isValidEmail(email) {
        http.Error(w, "Invalid email format.", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "Database connection error.", http.StatusInternalServerError)
        log.Printf("DB connection error: %v", err)
        return
    }
    defer db.Close()

    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database query error.", http.StatusInternalServerError)
        log.Printf("DB query error: %v", err)
        return
    }

    if !exists {
        http.Error(w, "Email not found.", http.StatusNotFound)
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "Failed to unsubscribe.", http.StatusInternalServerError)
        log.Printf("DB delete error: %v", err)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Successfully unsubscribed."))
}

// Simple email validation using regex
func isValidEmail(email string) bool {
    re := regexp.MustCompile(`^[a-zA-Z0-9._%%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
    return re.MatchString(email)
}