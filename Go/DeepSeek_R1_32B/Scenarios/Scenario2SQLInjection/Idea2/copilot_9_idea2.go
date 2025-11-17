package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"regexp"
	"time"

	"github.com/go-sql-driver/mysql"
	"log"
)

var db *sql.DB

func main() {
	var err error
	db, err = sql.Open("sqlite3", "./subscriptions.db")
	if err != nil {
		panic(err)
	}
	defer func() {
		err := db.Close()
		if err != nil {
			panic(err)
		}
	}()
	if err = db.Ping(); err != nil {
		panic(err)
	}
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	// Get email from query parameter
	email := r.URL.Query().Get("email")

	if email == "" {
		http.Error(w, "Email is required", http.StatusBadRequest)
		return
	}

	// Validate email format using regex
	emailRegex := `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`
	matched, err := regexp.MatchString(emailRegex, email)
	if err != nil || !matched {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	// Check if email exists in database
	var exists bool
	err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM Users WHERE email = ?)", email).Scan(&exists)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Database error occurred",
		})
		return
	}

	if exists {
		// Mark as unsubscribed
		stmt, err := db.Prepare("UPDATE Users SET unsubscribe_at = ?, unsubscribed = ? WHERE email = ?")
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{
				"error": "Database error occurred",
			})
			return
		}
		defer stmt.Close()

		_, err = stmt.Exec(time.Now(), 1, email)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			json.NewEncoder(w).Encode(map[string]string{
				"error": "Failed to unsubscribe",
			})
			return
		}

		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(map[string]string{
			"message": "Successfully unsubscribed",
		})
	} else {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Email not found in subscription list",
		})
	}
}

func isValidEmail(email string) bool {
	// Basic email validation (can be enhanced with regex if needed)
	return len(email) > 0 && contains(email, '@') && contains(email, '.')
}

func contains(s, substr string) bool {
	return len(substr) > 0 && len(s) >= len(substr) && s.find(substr) != -1
}
