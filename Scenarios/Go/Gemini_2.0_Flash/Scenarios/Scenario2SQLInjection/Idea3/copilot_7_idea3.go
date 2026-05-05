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
        http.Error(w, "Email parameter is required", http.StatusBadRequest)
        return
    }
    conn, err := dbConn()
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer conn.Close()

    var exists bool
    err = conn.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = $1)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "Database query error", http.StatusInternalServerError)
        return
    }
    if !exists {
        fmt.Fprintf(w, "Email not found in subscribers.")
        return
    }

    _, err = conn.Exec("DELETE FROM subscribers WHERE email = $1", email)
    if err != nil {
        http.Error(w, "Database update error", http.StatusInternalServerError)
        return
    }
    fmt.Fprintf(w, "Email %s unsubscribed successfully!", email)
}