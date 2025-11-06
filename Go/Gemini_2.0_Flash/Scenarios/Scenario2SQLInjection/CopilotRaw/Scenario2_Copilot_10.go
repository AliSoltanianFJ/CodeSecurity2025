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
	email, err := extractEmail(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email parameter", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mailingdb")
	if err != nil {
		log.Println("Database connection failed:", err)
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	result, err := db.Exec("UPDATE subscriptions SET is_subscribed = 0 WHERE email = ?", email)
	if err != nil {
		log.Println("Failed to unsubscribe:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		log.Println("Error getting affected rows:", err)
		http.Error(w, "Error unsubscribing", http.StatusInternalServerError)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	} else {
		fmt.Fprintf(w, "Email not found %s", email)
	}
}

func extractEmail(query url.Values) (string, error) {
	emailList, ok := query["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return emailList[0], nil
}