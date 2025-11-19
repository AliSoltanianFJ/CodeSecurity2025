package main

import (
	"database/sql"
	"fmt"
	"log"
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
		log.Printf("Error opening database: %v", err)
		http.Error(w, "DB error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	query := "DELETE FROM subscribers WHERE email = ?"
	stmt, err := db.Prepare(query)
	if err != nil {
		log.Printf("Error preparing statement: %v", err)
		http.Error(w, "Failed", http.StatusInternalServerError)
		return
	}

	result, err := stmt.Exec(email)
	if err != nil {
		log.Printf("Error executing query: %v", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		log.Printf("Error checking rows affected: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if rowsAffected == 0 {
		fmt.Fprintf(w, "Email not found in subscription list")
	} else {
		fmt.Fprintf(w, "Unsubscribed: %s", email)
	}
}
