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
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/subscriptions")
	if err != nil {
		log.Println("Failed to connect to database:", err)
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	email, err := extractEmail(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email parameter", http.StatusBadRequest)
		return
	}

	err = unsubscribeEmail(db, email)
	if err != nil {
		log.Println("Failed to unsubscribe email:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
}

func extractEmail(query url.Values) (string, error) {
	emailList, ok := query["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("email parameter is missing")
	}
	return emailList[0], nil
}

func unsubscribeEmail(db *sql.DB, email string) error {
	query := "UPDATE subscribers SET active = 0 WHERE email = ?"
	_, err := db.Exec(query, email)
	return err
}