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

	var exists bool
	err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
	if err != nil {
		http.Error(w, "Failed to check subscription", http.StatusInternalServerError)
		return
	}

	if !exists {
		http.Error(w, "Email not found", http.StatusNotFound)
		return
	}

	stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
	if err != nil {
		http.Error(w, "Prepare delete error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	res, err := stmt.Exec(email)
	if err != nil {
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := res.RowsAffected()
	if err != nil || rowsAffected == 0 {
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
}
