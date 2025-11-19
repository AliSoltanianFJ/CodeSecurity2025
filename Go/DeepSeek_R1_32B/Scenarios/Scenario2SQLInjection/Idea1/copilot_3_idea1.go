package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"os"

	_ "github.com/mattn/go-sqlite3"
)

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")
	if email == "" {
		http.Error(w, "Email required", http.StatusBadRequest)
		return
	}

	dbPath := os.Getenv("DB_PATH")
	if dbPath == "" {
		dbPath = "./subscribers.db"
	}

	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	result, err := stmt.Exec(email)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if rowsAffected == 0 {
		fmt.Fprintf(w, "Email not found")
	} else {
		fmt.Fprintf(w, "Successfully unsubscribed")
	}
}

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}
