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
	http.HandleFunc("/unsubscribe", unsubscribeHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		log.Printf("Database connection failed: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Printf("Database ping failed: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	email, err := getEmail(r.URL.Query())
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	unsubscribed, err := unsubscribeEmail(db, email)
	if err != nil {
		log.Printf("Unsubscribe operation failed: %v", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	if unsubscribed {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	} else {
		fmt.Fprintf(w, "Email address %s not found", email)
	}
}

func getEmail(values url.Values) (string, error) {
	emailList, ok := values["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("email parameter is required")
	}
	return emailList[0], nil
}

func unsubscribeEmail(db *sql.DB, email string) (bool, error) {
	result, err := db.Exec("UPDATE users SET subscribed = 0 WHERE email = ?", email)
	if err != nil {
		return false, err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return false, err
	}

	return rowsAffected > 0, nil
}