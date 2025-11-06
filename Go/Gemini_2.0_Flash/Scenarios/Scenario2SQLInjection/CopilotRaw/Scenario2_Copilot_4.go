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
	email, err := extractEmailFromRequest(r)
	if err != nil {
		http.Error(w, "Invalid request: "+err.Error(), http.StatusBadRequest)
		return
	}

	err = unsubscribe(email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		log.Println("Unsubscribe error:", err)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed %s", email)
}

func extractEmailFromRequest(r *http.Request) (string, error) {
	queryValues := r.URL.Query()
	emailList, ok := queryValues["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return emailList[0], nil
}

func unsubscribe(email string) error {
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mailinglist")
	if err != nil {
		return fmt.Errorf("database connection error: %w", err)
	}
	defer db.Close()

	_, err = db.Exec("UPDATE subscriptions SET active = 0 WHERE email = ?", email)
	if err != nil {
		return fmt.Errorf("unsubscribe query failed: %w", err)
	}

	return nil
}