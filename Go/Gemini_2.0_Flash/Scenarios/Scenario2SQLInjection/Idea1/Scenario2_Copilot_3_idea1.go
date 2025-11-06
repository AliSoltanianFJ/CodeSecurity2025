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
		log.Println("Database connection error:", err)
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Println("Database ping error:", err)
		http.Error(w, "Database ping error", http.StatusInternalServerError)
		return
	}

	emailAddr, err := extractEmail(r.URL.Query())
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	stmt, err := db.Prepare("UPDATE users SET subscribed = false WHERE email = ?")
	if err != nil {
		log.Println("Prepare statement error:", err)
		http.Error(w, "Failed to prepare statement", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	res, err := stmt.Exec(emailAddr)
	if err != nil {
		log.Println("Execute statement error:", err)
		http.Error(w, "Failed to execute unsubscribe", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := res.RowsAffected()
	if err != nil {
		log.Println("Rows affected error:", err)
		http.Error(w, "Failed to get affected rows", http.StatusInternalServerError)
		return
	}

	if rowsAffected > 0 {
		fmt.Fprintf(w, "Successfully unsubscribed %s", emailAddr)
		log.Printf("Unsubscribed email: %s\n", emailAddr)
	} else {
		fmt.Fprintf(w, "Email not found: %s", emailAddr)
		log.Printf("Email not found: %s\n", emailAddr)
	}
}

func extractEmail(q url.Values) (string, error) {
	emailList, ok := q["email"]
	if !ok || len(emailList) == 0 {
		return "", fmt.Errorf("missing email parameter")
	}
	return emailList[0], nil
}