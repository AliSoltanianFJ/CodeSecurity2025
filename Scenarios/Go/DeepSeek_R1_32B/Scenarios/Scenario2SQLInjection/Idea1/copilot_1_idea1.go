package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"strings"
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

	query := "DELETE FROM subscribers WHERE email = ?"
	stmt, err := db.Prepare(query)
	if err != nil {
		http.Error(w, "Prepare statement failed", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	res, err := stmt.Exec(email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	count, err := res.RowsAffected()
	if err != nil {
		http.Error(w, "Error checking subscription status", http.StatusInternalServerError)
		return
	}

	if count > 0 {
		fmt.Fprintf(w, "Unsubscribed: %s\n", email)
	} else {
		http.Error(w, "Email not found in subscribers", http.StatusNotFound)
	}
}

func init() {
	fmt.Printf("Starting unsubscribe service on :8080\n")
}
