package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"regexp"
)

const (
	dbName = "your_database.db" // Replace with your actual database name/path
)

// sanitizeEmail removes any potentially harmful characters from the email string
func sanitizeEmail(email string) string {
	return regexp.MustCompile(`[^a-zA-Z0-9.@_-]`).ReplaceAllString(email, "")
}

// validateEmail checks if the email format is valid
func validateEmail(email string) bool {
	matched, _ := regexp.MatchString(`^\S+@\S+\.\S+$`, email)
	return matched
}

type Response struct {
	Status  string `json:"status"`
	Message string `json:"message"`
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")

	// Check if email parameter is provided
	if email == "" {
		w.WriteHeader(http.StatusBadRequest)
		response := Response{Status: "error", Message: "Email parameter is required"}
		json.NewEncoder(w).Encode(response)
		return
	}

	// Validate email format
	if !validateEmail(email) {
		w.WriteHeader(http.StatusBadRequest)
		response := Response{Status: "error", Message: "Invalid email format"}
		json.NewEncoder(w).Encode(response)
		return
	}

	// Sanitize the email input to prevent any potential attacks
	safeEmail := sanitizeEmail(email)

	db, err := sql.Open("sqlite3", dbName)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		response := Response{Status: "error", Message: "Internal server error"}
		json.NewEncoder(w).Encode(response)
		return
	}
	defer db.Close()

	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM subscribers WHERE email=?", safeEmail).Scan(&count)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		response := Response{Status: "error", Message: "Internal server error"}
		json.NewEncoder(w).Encode(response)
		return
	}

	if count == 0 {
		w.WriteHeader(http.StatusBadRequest)
		response := Response{Status: "error", Message: "No subscription found for this email"}
		json.NewEncoder(w).Encode(response)
	} else {
		w.WriteHeader(http.StatusOK)
		response := Response{Status: "success", Message: "Successfully unsubscribed"}
		json.NewEncoder(w).Encode(response)
	}
}

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}
