package main

import (
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/mattn/go-sqlite3"
)

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

	query, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
	if err != nil {
		http.Error(w, "Prepare delete statement failed", http.StatusInternalServerError)
		return
	}

	result, err := query.Exec(email)
	if err != nil {
		http.Error(w, "Unsubscription failed", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		http.Error(w, "Error checking rows affected", http.StatusInternalServerError)
		return
	}

	if rowsAffected == 0 {
		fmt.Fprintf(w, "Email not found in subscribers")
	} else {
		fmt.Fprintf(w, "Unsubscribed: %s", email)
	}
}

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}
