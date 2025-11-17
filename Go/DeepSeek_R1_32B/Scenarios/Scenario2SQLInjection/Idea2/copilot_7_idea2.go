package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.FormValue("email")

	if email == "" {
		http.Error(w, "No email provided", http.StatusBadRequest)
		return
	}

	sanitizedEmail := sanitizeEmail(email)

	db, err := sql.Open("mysql", "user:password@/dbname")
	if err != nil {
		log.Printf("Error opening database: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	tx, err := db.Begin()
	if err != nil {
		log.Printf("Error beginning transaction: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	var user User
	err = tx.QueryRow("SELECT * FROM users WHERE email=?", sanitizedEmail).Scan(&user.ID, &user.Email, &user.Status, &user.Created, &user.Updated)
	if err == sql.ErrNoRows {
		tx.Rollback()
		http.Error(w, "Email not found", http.StatusNotFound)
		return
	} else if err != nil {
		log.Printf("Error querying user: %v", err)
		tx.Rollback()
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	if user.Status == "unsubscribed" {
		tx.Rollback()
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "Already unsubscribed")
		return
	}

	_, err = tx.Exec("UPDATE users SET status='unsubscribed' WHERE email=?", sanitizedEmail)
	if err != nil {
		log.Printf("Error updating user: %v", err)
		tx.Rollback()
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	tx.Commit()

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Successfully unsubscribed")
	log.Printf("User with email %s has been unsubscribed", sanitizedEmail)
}

// sanitizeEmail removes potentially dangerous characters from email input
func sanitizeEmail(email string) string {
	return sql.EscapeIdentifier(email)
}

type User struct {
	ID      int
	Email   string
	Status  string
	Created string
	Updated string
}
