package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"regexp"
	"time"

	"github.com/gorilla/mux"
	_ "github.com/lib/pq"
)

var db *sql.DB

func main() {
	router := mux.NewRouter()
	router.HandleFunc("/unsubscribe", unsubscribe).Methods("GET")
	http.ListenAndServe(":8080", router)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	email := r.FormValue("email")

	// Validate input
	if email == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "Email is required"})
		return
	}

	// Sanitize input to prevent SQL injection and other attacks
	sanitizedEmail, err := sanitizeInput(email)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "Invalid email format"})
		return
	}

	db = OpenDB()

	// Use prepared statement to prevent SQL injection
	stmt, err := db.Prepare("SELECT id FROM subscribers WHERE email = ?")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "Database error"})
		return
	}
	defer stmt.Close()

	var subscriberID int
	err = stmt.QueryRow(sanitizedEmail).Scan(&subscriberID)
	if err == sql.ErrNoRows {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "Email not found in our records"})
		return
	} else if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "Database error"})
		return
	}

	// Mark user as unsubscribed and record the unsubscribe date
	updateStmt, err := db.Prepare("UPDATE subscribers SET is_subscribed = FALSE, unsubscribed_at = ? WHERE id = ?")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "Database error"})
		return
	}
	defer updateStmt.Close()

	_, err = updateStmt.Exec(time.Now(), subscriberID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": "Unable to process unsubscribe request"})
		return
	}

	db.Close()

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{"message": "Successfully unsubscribed"})
}

// sanitizeInput cleans and validates the input email
func sanitizeInput(email string) (string, error) {
	// Trim whitespace and check for empty string
	email = strings.TrimSpace(email)
	if email == "" {
		return "", fmt.Errorf("empty email")
	}

	// Basic email validation
	if !isEmailValid(email) {
		return "", fmt.Errorf("invalid email format")
	}

	return email, nil
}

// isEmailValid performs basic email format validation
func isEmailValid(email string) bool {
	emailRX := regexp.MustCompile(`^[^\s@]+@[^\s@]+\.[^\s@]+$`)
	return emailRX.MatchString(email)
}

// OpenDB connects to the database and returns a *sql.DB pointer
func OpenDB() *sql.DB {
	db, err := sql.Open("postgres", "user=postgres dbname=subscribers sslmode=disable")
	if err != nil {
		panic(err)
	}
	return db
}
