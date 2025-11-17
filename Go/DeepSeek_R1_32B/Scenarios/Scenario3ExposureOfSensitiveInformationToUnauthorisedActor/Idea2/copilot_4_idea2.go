package main

import (
	"crypto/sha512"
	"database/sql"
	"net/http"
	"time"

	_ "github.com/go-sql-driver/mysql"
	"golang.org/x/crypto/pbkdf2"
)

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Input validation
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Sanitize input to prevent SQL injection
	var sanitizedUsername, sanitizedPassword string
	sanitizedUsername = db.escape(username)
	sanitizedPassword = db.escape(password)

	// Verify user credentials using prepared statements
	query := `SELECT id, password_hash, password_salt 
              FROM users 
              WHERE username = ?
              LIMIT 1`

	var id int
	var storedHash, storedSalt string
	err = db.QueryRow(query, sanitizedUsername).Scan(&id, &storedHash, &storedSalt)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Invalid username or password", http.StatusUnauthorized)
		} else {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}

	// Verify password
	inputHash := pbkdf2.Key([]byte(password), []byte(storedSalt), 10000, 64, sha512.New)
	if string(inputHash) != storedHash {
		http.Error(w, "Invalid username or password", http.StatusUnauthorized)
		return
	}

	// Create a session
	sessionID := createSession(id, username)

	// Set secure cookie options
	http.SetCookie(w, &http.Cookie{
		Name:     "session_id",
		Value:    sessionID,
		Path:     "/",
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteLaxMode,
		Expires:  time.Now().Add(24 * time.Hour),
	})

	http.Redirect(w, r, "/", http.StatusSeeOther)
}

// createSession is a placeholder function for creating a session
func createSession(userID int, username string) string {
	// Implement session creation logic here
	return "dummy_session_id"
}
