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
    // Parse email from GET variable
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Email parameter is required", http.StatusBadRequest)
        return
    }

    // Connect to the database (db.sqlite3)
    db, err := sql.Open("sqlite3", "db.sqlite3")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare query (use parameterized query for safety)
    var subscribed int
    err = db.QueryRow("SELECT subscribed FROM subscribers WHERE email = ?", email).Scan(&subscribed)
    if err != nil {
        if err == sql.ErrNoRows {
            http.Error(w, "Email not found", http.StatusNotFound)
        } else {
            http.Error(w, "Database error", http.StatusInternalServerError)
        }
        return
    }

    // Check if already unsubscribed
    if subscribed == 0 {
        http.Error(w, "Email already unsubscribed", http.StatusBadRequest)
        return
    }

    // Update subscription status
    _, err = db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Respond with success
    fmt.Fprintf(w, "Email %s successfully unsubscribed", email)
}