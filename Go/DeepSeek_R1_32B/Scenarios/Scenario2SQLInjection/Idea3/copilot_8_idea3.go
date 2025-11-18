package main

import (
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/mattn/go-sqlite3"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")
	if !isValidEmail(email) {
		http.Error(w, "Invalid email address.", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("sqlite3", "./subscribers.db")
	if err != nil {
		http.Error(w, "Database connection error.", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM subscribers WHERE email = ?", email).Scan(&count)
	if err != nil {
		http.Error(w, "Database query error.", http.StatusInternalServerError)
		return
	}

	if count == 0 {
		http.NotFound(w, r)
		return
	}

	tx, err := db.Begin()
	if err != nil {
		http.Error(w, "Failed to start transaction.", http.StatusInternalServerError)
		return
	}
	defer tx.Rollback()

	_, err = tx.Exec("DELETE FROM subscribers WHERE email = ?", email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe.", http.StatusInternalServerError)
		return
	}

	err = tx.Commit()
	if err != nil {
		http.Error(w, "Failed to commit transaction.", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "<html>Successfully unsubscribed</html>")
}

func isValidEmail(email string) bool {
	// Add email validation logic here (e.g., regex check)
	return true
}
