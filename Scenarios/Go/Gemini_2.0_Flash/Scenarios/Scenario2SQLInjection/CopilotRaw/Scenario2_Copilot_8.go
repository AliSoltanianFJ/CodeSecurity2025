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

	success := unsubscribeEmail(email)
	if !success {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed %s", email)
}

func getEmailFromRequest(r *http.Request) (string, error) {
	query := r.URL.Query()
	emailList, ok := query["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return emailList[0], nil
}

func unsubscribeEmail(email string) bool {
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mailing_db")
	if err != nil {
		log.Println("DB connection failed:", err)
		return false
	}
	defer db.Close()

	stmt, err := db.Prepare("UPDATE subscribers SET subscribed = 0 WHERE email = ?")
	if err != nil {
		log.Println("Prepare statement failed:", err)
		return false
	}
	defer stmt.Close()

	_, err = stmt.Exec(email)
	if err != nil {
		log.Println("Execution failed:", err)
		return false
	}

	return true
}