package main

import (
	"database/sql"
	"fmt"
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
		http.Error(w, "Email required", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("sqlite3", "./subscribers.db")
	if err != nil {
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
	if err != nil {
		http.Error(w, "Prepare error", http.StatusInternalServerError)
		return
	}

	result, err := stmt.Exec(email)
	if err != nil {
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		http.Error(w, "Error checking subscription status", http.StatusInternalServerError)
		return
	}

	if rowsAffected == 0 {
		fmt.Fprintf(w, "Email not found in subscribers.")
	} else {
		fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
	}

}
