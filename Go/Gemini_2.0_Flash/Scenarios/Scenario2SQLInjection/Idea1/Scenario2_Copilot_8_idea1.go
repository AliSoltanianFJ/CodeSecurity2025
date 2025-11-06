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
		log.Printf("DB connection failed: %v", err)
		http.Error(w, "DB connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	if err = db.Ping(); err != nil {
		log.Printf("DB ping failed: %v", err)
		http.Error(w, "DB ping failed", http.StatusInternalServerError)
		return
	}

	email, err := extractEmail(r.URL.Query())
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	err = unsubscribeUser(db, email)
	if err != nil {
		log.Printf("Unsubscribe failed: %v", err)
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	log.Printf("Successfully unsubscribed %s", email)
}

func extractEmail(values url.Values) (string, error) {
	emailList, ok := values["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("email parameter missing")
	}
	return emailList[0], nil
}

func unsubscribeUser(db *sql.DB, email string) error {
	result, err := db.Exec("UPDATE users SET subscribed = false WHERE email = ?", email)
	if err != nil {
		return err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return err
	}

	if rowsAffected == 0 {
		return fmt.Errorf("email not found")
	}

	return nil
}