package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"net/url"

	_ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
	var err error
	db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/email_list")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	err = db.Ping()
	if err != nil {
		log.Fatal(err)
	}

	http.HandleFunc("/unsubscribe", unsubscribeHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
	email, err := getEmail(r.URL.Query())
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	err = unsubscribeEmail(email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		log.Println("Unsubscribe failed:", err)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed %s", email)
}

func getEmail(query url.Values) (string, error) {
	emails, ok := query["email"]
	if !ok || len(emails) == 0 {
		return "", fmt.Errorf("email is required")
	}
	return emails[0], nil
}

func unsubscribeEmail(email string) error {
	_, err := db.Exec("UPDATE subscribers SET is_active = 0 WHERE email = ?", email)
	return err
}