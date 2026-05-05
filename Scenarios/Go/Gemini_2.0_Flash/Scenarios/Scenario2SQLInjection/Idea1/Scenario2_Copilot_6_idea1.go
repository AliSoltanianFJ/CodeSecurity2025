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
	email, err := getEmailFromRequest(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		log.Printf("Error connecting to database: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	err = db.Ping()
	if err != nil {
		log.Printf("Error pinging database: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := unsubscribeUser(db, email)
	if err != nil {
		log.Printf("Error unsubscribing user: %v", err)
		http.Error(w, "Unsubscribe error", http.StatusInternalServerError)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	} else {
		fmt.Fprintf(w, "Email not found: %s", email)
	}
}

func getEmailFromRequest(r *http.Request) (string, error) {
	queryValues := r.URL.Query()
	emailList, ok := queryValues["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return emailList[0], nil
}

func unsubscribeUser(db *sql.DB, email string) (int64, error) {
	result, err := db.Exec("UPDATE users SET subscribed = false WHERE email = ?", email)
	if err != nil {
		return 0, err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return 0, err
	}

	return rowsAffected, nil
}