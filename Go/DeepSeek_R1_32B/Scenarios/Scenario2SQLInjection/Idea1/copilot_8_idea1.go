package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	_ "github.com/mattn/go-sqlite3"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

type UnsubscribeResponse struct {
	Message string `json:"message"`
	Status  bool   `json:"status"`
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")
	if email == "" {
		http.Error(w, "Email required", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("sqlite3", "./subscribers.db")
	if err != nil {
		log.Printf("Error opening database: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	stmt, err := db.Prepare("DELETE FROM subscribers WHERE email = ?")
	if err != nil {
		log.Printf("Error preparing statement: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	result, err := stmt.Exec(email)
	if err != nil {
		log.Printf("Error executing query: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		log.Printf("Error checking affected rows: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	response := &UnsubscribeResponse{
		Message: fmt.Sprintf("Unsubscribed: %s", email),
		Status:  rowsAffected > 0,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		log.Printf("Error encoding response: %v", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
}
