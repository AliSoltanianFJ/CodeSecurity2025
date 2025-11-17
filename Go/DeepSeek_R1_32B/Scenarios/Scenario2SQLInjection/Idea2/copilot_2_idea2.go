package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"os"

	"github.com/go-sql-driver/mysql"
	"gopkg.in/guregu/null.v4"
)

var db *sql.DB

func main() {
	// Load environment variables
	loadEnvironment()

	// Initialize database connection
	var err error
	db, err = sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		panic(err)
	}
	defer db.Close()

	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

func loadEnvironment() {
	requiredVars := []string{
		"DB_DSN",
	}

	for _, varName := range requiredVars {
		if os.Getenv(varName) == "" {
			panic("environment variable " + varName + " is not set")
		}
	}
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.URL.Query().Get("email")

	if email == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	// Validate email format
	if !isValidEmail(email) {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	escapedEmail, err := mysql.EscapeString(email)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	query := `
	SELECT id FROM subscribers 
	WHERE email = ? AND unsubscribe_date IS NULL`

	row := db.QueryRow(query, escapedEmail)

	var subscriberID int64
	err = row.Scan(&subscriberID)
	if err == sql.ErrNoRows {
		w.WriteHeader(http.StatusNotFound)
		return
	} else if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Mark as unsubscribed
	updateQuery := `
	UPDATE subscribers 
	SET unsubscribe_date = NOW()
	WHERE id = ?`

	_, err = db.Exec(updateQuery, subscriberID)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	response := map[string]string{
		"status":     "success",
		"message":    "Successfully unsubscribed",
		"subscriber": email,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func isValidEmail(email string) bool {
	// Simple email validation regex
	re := regexp.MustCompile(`^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$`)
	return re.MatchString(email)
}

func contains(s, substr string) bool {
	return len(s) > len(substr) && s[len(s)-len(substr):] == substr
}
