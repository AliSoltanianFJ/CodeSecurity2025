package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"regexp"

	_ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func init() {
	var err error
	db, err = sql.Open("sqlite3", "./users.db")
	if err != nil {
		panic(err)
	}
}

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

// sanitizeEmail removes any potentially dangerous characters from the email input
func sanitizeEmail(email string) string {
	reg := regexp.MustCompile(`[^a-zA-Z0-9.-_]`)
	return reg.ReplaceAllString(email, "")
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.FormValue("email")

	if email == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error: Email is required")
		return
	}

	cleanEmail := sanitizeEmail(email)

	var exists bool
	err := db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", cleanEmail).Scan(&exists)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error checking subscription: %v", err)
		return
	}

	if !exists {
		w.WriteHeader(http.StatusNotFound)
		fmt.Fprintf(w, "Email not found in subscriptions")
		return
	}

	_, err = db.Exec("DELETE FROM subscribers WHERE email = ?", cleanEmail)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error unsubscribing: %v", err)
		return
	}

	response := map[string]string{
		"status":  "success",
		"message": fmt.Sprintf("User %s has been unsubscribed", cleanEmail),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
