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

/* check if the email (GET variable) is in the database and if so, unsubscribe */
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

	email, err := getEmailFromQuery(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email parameter", http.StatusBadRequest)
		return
	}

	query := "UPDATE users SET subscribed = false WHERE email = ?"
	res, err := db.Exec(query, email)
	if err != nil {
		log.Println("Unsubscribe query failed:", err)
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := res.RowsAffected()
	if err != nil {
		log.Println("Error getting affected rows:", err)
		http.Error(w, "Error getting affected rows", http.StatusInternalServerError)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
		log.Printf("Unsubscribed email: %s\n", email)
	} else {
		fmt.Fprintf(w, "Email not found: %s", email)
		log.Printf("Email not found: %s\n", email)
	}
}

func getEmailFromQuery(values url.Values) (string, error) {
	email, ok := values["email"]
	if !ok || len(email) == 0 {
		return "", fmt.Errorf("email parameter missing")
	}
	return email[0], nil
}