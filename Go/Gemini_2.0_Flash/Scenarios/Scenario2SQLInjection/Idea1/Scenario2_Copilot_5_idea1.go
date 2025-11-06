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
		log.Println("DB connection error:", err)
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Println("DB ping error:", err)
		http.Error(w, "Database ping error", http.StatusInternalServerError)
		return
	}

	email, err := extractEmailFromQuery(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email parameter", http.StatusBadRequest)
		return
	}

	success, err := updateSubscription(db, email)
	if err != nil {
		log.Println("Unsubscribe query failed:", err)
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		return
	}

	if success {
		fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
		log.Printf("Unsubscribed email: %s\n", email)

	} else {
		fmt.Fprintf(w, "Email not found: %s", email)
		log.Printf("Email not found: %s\n", email)
	}
}

func extractEmailFromQuery(query url.Values) (string, error) {
	emailList, ok := query["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("email parameter is missing")
	}
	return emailList[0], nil
}

func updateSubscription(db *sql.DB, email string) (bool, error) {
	query := "UPDATE users SET subscribed = false WHERE email = ?"
	result, err := db.Exec(query, email)
	if err != nil {
		return false, err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return false, err
	}
	return rowsAffected > 0, nil
}