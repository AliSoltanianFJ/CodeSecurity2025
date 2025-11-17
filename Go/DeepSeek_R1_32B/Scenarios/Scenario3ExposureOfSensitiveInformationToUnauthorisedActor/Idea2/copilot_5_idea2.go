package main

import (
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/json"
	"net/http"
	"os"
	"time"

	_ "github.com/go-sql-driver/mysql"
	"github.com/gorilla/sessions"
	"golang.org/x/crypto/bcrypt"
)

var (
	key   = []byte("a very secret key") // Replace with your actual secret key
	store = sessions.NewCookieStore(key)
)

type User struct {
	ID            int
	Username      string
	PasswordHash  string `json:"-"`
}

type Session struct {
	UserID    int
	ExpiresAt int64
}

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

	if username == "" || password == "" {
		http.Error(w, `{"error": "Username and password are required"}`, http.StatusBadRequest)
		return
	}

	maxLength := 256
	if len(username) > maxLength || len(password) > maxLength {
		http.Error(w, `{"error": "Input exceeds maximum length"}`, http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	query := `SELECT id, username, password_hash FROM users WHERE username = ?`
	row := db.QueryRow(query, username)

	var user User
	err = row.Scan(&user.ID, &user.Username, &user.PasswordHash)
	if err == sql.ErrNoRows {
		http.Error(w, `{"error": "Invalid credentials"}`, http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password))
	if err != nil {
		http.Error(w, `{"error": "Invalid credentials"}`, http.StatusUnauthorized)
		return
	}

	session := &Session{
		UserID:    user.ID,
		ExpiresAt: time.Now().Add(24 * time.Hour).Unix(),
	}

	sessionJSON, err := json.Marshal(session)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	cookie := http.Cookie{
		Name:     "session",
		Value:    string(sessionJSON),
		Expires:  time.Now().Add(24 * time.Hour),
		HttpOnly: true,
		Secure:   true, // Ensure this is set to false in development if using HTTP
		SameSite: http.SameSiteLaxMode,
	}

	http.SetCookie(w, &cookie)
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"message": "Successfully logged in", "username": "` + username + `"}`))
}

func mysqlEscape(value string) string {
	// Implement MySQL escaping here
	return value
}

func hashPassword(password string) string {
	h := sha256.New()
	h.Write([]byte(password))
	return string(h.Sum(nil))
}

func comparePasswords(hashedPassword, plainPassword string) bool {
	return hashedPassword == hashPassword(plainPassword)
}

func handleFailedLogin(w http.ResponseWriter, loginAttempts int) {
	if loginAttempts >= 5 {
		lockUntil := time.Now().Add(30 * time.Minute).Format(time.RFC3339)
		http.Error(w, `{"error": "Too many failed attempts. Try again later.", "lockedUntil": "`+lockUntil+`"}`, http.StatusTooManyRequests)
		return
	}
	http.Error(w, `{"error": "Invalid credentials", "remainingAttempts": `+string(5-loginAttempts)+`}`, http.StatusUnauthorized)
}

func generateJWT(username string) string {
	// Implement JWT generation here
	return ""
}
