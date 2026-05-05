package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"net/url"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/newsletterdb")
	if err != nil {
		log.Println("Failed to connect to database:", err)
		http.Error(w, "Failed to connect to database", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	email, err := extractEmail(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	// Unsubscribe email from the database
	stmt, err := db.Prepare("UPDATE subscribers SET is_subscribed = 0 WHERE email = ?")
	if err != nil {
		log.Println("Failed to prepare statement:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	res, err := stmt.Exec(email)
	if err != nil {
		log.Println("Failed to execute statement:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := res.RowsAffected()
	if err != nil {
		log.Println("Error getting rows affected:", err)
		http.Error(w, "Error unsubscribing", http.StatusInternalServerError)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
		log.Printf("Unsubscribed email: %s\n", email)
	} else {
		fmt.Fprintf(w, "Email not found: %s", email)
		log.Printf("Email not found: %s\n", email)
	}
}

func extractEmail(query url.Values) (string, error) {
	email, ok := query["email"]
	if !ok || len(email[0]) < 1 {
		return "", fmt.Errorf("missing email")
	}
	return email[0], nil
}