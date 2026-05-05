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
		http.Error(w, "Invalid request: "+err.Error(), http.StatusBadRequest)
		return
	}

	db, err := getDBConnection()
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		log.Println("DB connection error:", err)
		return
	}
	defer db.Close()

	err = unsubscribeEmail(db, email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		log.Println("Unsubscribe error:", err)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed %s", email)
}

func getEmailFromRequest(r *http.Request) (string, error) {
	queryParams := r.URL.Query()
	emailValues, ok := queryParams["email"]
	if !ok || len(emailValues) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return emailValues[0], nil
}

func getDBConnection() (*sql.DB, error) {
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mailing_list")
	return db, err
}

func unsubscribeEmail(db *sql.DB, email string) error {
	_, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
	return err
}