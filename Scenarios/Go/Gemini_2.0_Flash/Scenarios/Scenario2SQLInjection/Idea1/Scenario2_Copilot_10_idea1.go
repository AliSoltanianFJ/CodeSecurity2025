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
	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		log.Printf("Failed to open database: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Printf("Failed to ping database: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	email, err := extractEmailAddress(r.URL.Query())
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	updated, err := updateSubscription(db, email)
	if err != nil {
		log.Printf("Failed to update subscription: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	if updated {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	} else {
		fmt.Fprintf(w, "Email address not found: %s", email)
	}
}

func extractEmailAddress(values url.Values) (string, error) {
	emailList, ok := values["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("email parameter is required")
	}
	return emailList[0], nil
}

func updateSubscription(db *sql.DB, email string) (bool, error) {
	res, err := db.Exec("UPDATE users SET subscribed = false WHERE email = ?", email)
	if err != nil {
		return false, fmt.Errorf("failed to execute query: %v", err)
	}

	rowsAffected, err := res.RowsAffected()
	if err != nil {
		return false, fmt.Errorf("failed to get affected rows: %v", err)
	}

	return rowsAffected > 0, nil
}