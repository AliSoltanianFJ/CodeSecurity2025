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
	email, err := getEmailParam(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email parameter", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/emails")
	if err != nil {
		log.Println("DB connect error:", err)
		http.Error(w, "DB connection failed", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	stmt, err := db.Prepare("UPDATE email_subscriptions SET subscribed = ? WHERE email = ?")
	if err != nil {
		log.Println("DB prepare error:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	res, err := stmt.Exec(false, email)
	if err != nil {
		log.Println("DB exec error:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := res.RowsAffected()
	if err != nil {
		log.Println("DB rows affected error:", err)
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
	} else {
		fmt.Fprintf(w, "Email address not found: %s", email)
	}
}

func getEmailParam(query url.Values) (string, error) {
	email, ok := query["email"]
	if !ok || len(email) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return email[0], nil
}