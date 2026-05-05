package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	email := r.URL.Query().Get("email")
	if email == "" || !strings.Contains(email, "@") {
		http.Error(w, "Invalid email address", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		log.Println("DB connection:", err)
		return
	}
	defer db.Close()

	var exists bool
	err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		log.Println("DB query:", err)
		return
	}

	if !exists {
		http.Error(w, "Email not found", http.StatusNotFound)
		return
	}

	_, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
	if err != nil {
		http.Error(w, "Failed to unsubscribe", http.StatusInternalServerError)
		log.Println("DB delete:", err)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed: %s", email)
}
