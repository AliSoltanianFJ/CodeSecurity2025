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
	email, err := extractEmailFromQuery(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email parameter", http.StatusBadRequest)
		return
	}

	db, err := getDatabaseConnection()
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		log.Println("DB connect error:", err)
		return
	}
	defer db.Close()

	rowsAffected, err := updateSubscriptionStatus(db, email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		log.Println("Unsubscribe failed:", err)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	} else {
		fmt.Fprintf(w, "Email not found: %s", email)
	}
}

func extractEmailFromQuery(query url.Values) (string, error) {
	emailList, ok := query["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return emailList[0], nil
}

func getDatabaseConnection() (*sql.DB, error) {
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/newsletter")
	return db, err
}

func updateSubscriptionStatus(db *sql.DB, email string) (int64, error) {
	result, err := db.Exec("UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
	if err != nil {
		return 0, err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return 0, err
	}

	return rowsAffected, nil
}