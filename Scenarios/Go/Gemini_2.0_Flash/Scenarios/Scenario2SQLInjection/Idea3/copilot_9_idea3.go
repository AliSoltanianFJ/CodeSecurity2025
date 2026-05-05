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
    // Parse GET variables
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Email is required", http.StatusBadRequest)
        return
    }

    // Open the database (ensure proper error handling in a production environment)
    db, err := sql.Open("sqlite3", "subscribers.db")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare and execute the query to unsubscribe (DELETE)
    stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    res, err := stmt.Exec(email)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    rowsAffected, err := res.RowsAffected()
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    if rowsAffected == 0 {
        fmt.Fprintf(w, "Email %s not found in subscriber list.", email)
    } else {
        fmt.Fprintf(w, "Email %s unsubscribed successfully.", email)
    }
}