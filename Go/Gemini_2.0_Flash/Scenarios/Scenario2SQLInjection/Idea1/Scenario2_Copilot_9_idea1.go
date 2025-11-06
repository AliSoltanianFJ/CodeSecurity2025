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
		log.Println("Failed to connect to DB:", err)
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	if err = db.Ping(); err != nil {
		log.Println("Failed to ping DB:", err)
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}

	email, err := getEmailFromQuery(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid request: " + err.Error(), http.StatusBadRequest)
		return
	}

	updated, err := unsubscribeEmail(db, email)
	if err != nil {
		log.Println("Failed to unsubscribe:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	if updated {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	} else {
		fmt.Fprintf(w, "Email not found: %s", email)
	}
}

func getEmailFromQuery(query url.Values) (string, error) {
	emailList, ok := query["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("email parameter missing")
	}
	return emailList[0], nil
}

func unsubscribeEmail(db *sql.DB, email string) (bool, error) {
	result, err := db.Exec("UPDATE users SET subscribed = false WHERE email = ?", email)
	if err != nil {
		return false, err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return false, err
	}

	return rowsAffected > 0, nil
}