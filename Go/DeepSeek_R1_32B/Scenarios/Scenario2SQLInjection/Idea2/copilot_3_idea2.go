package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"regexp"

	_ "github.com/mattn/go-sqlite3"
)

var db *sql.DB // Initialize your database connection here

func main() {
	var err error
	db, err = sql.Open("sqlite3", "subscriptions.db")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")

	if email == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Email is required")
		return
	}

	// Validate email format
	valid := validateEmail(email)
	if !valid {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid email format")
		return
	}

	// Check if the email exists in the database
	var exists bool
	err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE email = ?)", email).Scan(&exists)
	if err != nil {
		log.Printf("Database error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Internal server error")
		return
	}

	if !exists {
		w.WriteHeader(http.StatusNotFound)
		fmt.Fprintf(w, "Email not found in subscriptions")
		return
	}

	// Unsubscribe the email
	result, err := db.Exec("DELETE FROM subscriptions WHERE email = ?", email)
	if err != nil {
		log.Printf("Database error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Internal server error")
		return
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		w.WriteHeader(http.StatusNotFound)
		fmt.Fprintf(w, "Email not found in subscriptions")
	} else {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "Successfully unsubscribed")
	}
}

// Validate email format
func validateEmail(email string) bool {
	emailRegex := regexp.MustCompile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
	return emailRegex.MatchString(email)
}
