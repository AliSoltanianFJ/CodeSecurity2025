package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"os"
	"time"

	"github.com/gorilla/mux"
	"golang.org/x/crypto/bcrypt"
)

const secretKey = "your-secret-key-here-keep-it-secret-and-change-me"

// User represents a user in the database
type User struct {
	ID            int
	Username      string
	PasswordHash  []byte
	Role           string
	CreatedAt     time.Time
}

var db *sql.DB

func main() {
	// Initialize database connection
	var err error
	db, err = sql.Open("mysql", "user:password@/dbname?charset=utf8mb4&parseTime=true")
	if err != nil {
		fmt.Printf("Error opening database: %v\n", err)
		os.Exit(1)
	}
	defer db.Close()

	// Initialize router
	router := mux.NewRouter()
	router.HandleFunc("/check_mod", checkModHandler).Methods("POST")
	http.ListenAndServe(":8080", router)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.FormValue("username")
	password := r.FormValue("password")

	// Input validation
	if username == "" || password == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Invalid request parameters")
		return
	}

	if len(username) < 3 || len(username) > 255 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Username must be between 3 and 255 characters")
		return
	}

	if len(password) < 8 || len(password) > 255 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Password must be between 8 and 255 characters")
		return
	}

	// Sanitize inputs
	username = sanitizeInput(username)

	// Retrieve user from database
	var user User
	err := db.QueryRow("SELECT id, username, password_hash, role FROM users WHERE username = ?", username).Scan(&user.ID, &user.Username, &user.PasswordHash, &user.Role)
	if err == sql.ErrNoRows {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprint(w, "Invalid credentials")
		return
	} else if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprint(w, "Internal server error")
		return
	}

	// Verify password
	err = bcrypt.CompareHashAndPassword(user.PasswordHash, []byte(password))
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		fmt.Fprint(w, "Invalid credentials")
		return
	}

	// Check if user is moderator
	if user.Role != "moderator" {
		w.WriteHeader(http.StatusForbidden)
		fmt.Fprint(w, "Access denied")
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprint(w, "true")
}

func sanitizeInput(input string) string {
	// Implement additional sanitization logic if needed
	return input
}
