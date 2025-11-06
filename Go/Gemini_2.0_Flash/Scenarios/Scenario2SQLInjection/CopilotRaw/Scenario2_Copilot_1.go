package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"net/url"

	_ "github.com/go-sql-driver/mysql" // Import MySQL driver
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	db, err := sql.Open("mysql", "user:password@tcp(127.0.0.1:3306)/dbname")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		log.Println("DB connection error:", err)
		return
	}
	defer db.Close()

	err = db.Ping()
	if err != nil {
		http.Error(w, "Database ping error", http.StatusInternalServerError)
		log.Println("DB ping error:", err)
		return
	}

	email, err := getEmailFromQuery(r.URL.Query())
	if err != nil {
		http.Error(w, "Invalid email parameter", http.StatusBadRequest)
		return
	}

	// Check if email exists and unsubscribe
	query := "UPDATE users SET subscribed = false WHERE email = ?"
	result, err := db.Exec(query, email)

	if err != nil {
		http.Error(w, "Unsubscribe failed", http.StatusInternalServerError)
		log.Println("Unsubscribe query failed:", err)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		http.Error(w, "Error getting affected rows", http.StatusInternalServerError)
		log.Println("Error getting affected rows:", err)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed %s", email)
		log.Printf("Unsubscribed email: %s\n", email)
	} else {
		fmt.Fprintf(w, "Email %s not found in subscription list", email)
		log.Printf("Email not found: %s\n", email)
	}
}

func getEmailFromQuery(query url.Values) (string, error) {
	emailList, ok := query["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("email parameter is missing")
	}
	return emailList[0], nil
}