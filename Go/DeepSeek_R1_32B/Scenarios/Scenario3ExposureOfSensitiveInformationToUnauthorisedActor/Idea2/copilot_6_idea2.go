package main

import (
	"crypto/rand"
	"crypto/sha512"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/dgrijalva/jwt-go"
	"golang.org/x/crypto/bcrypt"

	_ "github.com/go-sql-driver/mysql"
)

const maxUsernameLength = 256
const maxPasswordLength = 1024

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

type User struct {
	ID       int
	Username string
	Password []byte // Store hashed password in database
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Validate input lengths to prevent buffer overflow attacks
	if len(username) == 0 || len(username) > maxUsernameLength {
		http.Error(w, "Invalid username length", http.StatusBadRequest)
		return
	}
	if len(password) == 0 || len(password) > maxPasswordLength {
		http.Error(w, "Invalid password length", http.StatusBadRequest)
		return
	}

	// Sanitize input to prevent SQL injection and XSS attacks
	sanitizedUsername := sanitizeInput(username)
	if sanitizedUsername == "" {
		http.Error(w, "Invalid username characters", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", os.Getenv("DB_CONNECTION_STRING"))
	if err != nil {
		fmt.Printf("Error opening database: %v\n", err)
		os.Exit(1)
	}
	defer db.Close()

	// Use prepared statement to prevent SQL injection
	query := `
        SELECT id, username, password 
        FROM users 
        WHERE username = ?
    `
	user := &User{}
	stmt, err := db.Prepare(query)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	err = stmt.QueryRow(sanitizedUsername).Scan(&user.ID, &user.Username, &user.Password)
	if err == sql.ErrNoRows {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	} else if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Verify password using bcrypt for secure comparison
	err = bcrypt.CompareHashAndPassword(user.Password, []byte(password))
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	// Generate JWT token
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.Claims{
		"username": user.Username,
		"exp":      time.Now().Add(time.Hour).Unix(),
	})

	tokenString, err := token.SignedString([]byte(os.Getenv("JWT_SECRET")))
	if err != nil {
		http.Error(w, "Token generation failed", http.StatusInternalServerError)
		return
	}

	// Set secure cookie
	http.SetCookie(w, &http.Cookie{
		Name:     "session",
		Value:    tokenString,
		Expires:  time.Now().Add(time.Hour),
		HttpOnly: true,
		Secure:   true,      // Only send over HTTPS
		SameSite: http.SameSiteLaxMode,
	})

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Logged in successfully")
}

func sanitizeInput(input string) string {
	// Whitelist allowed characters for username
	for _, c := range input {
		if !(c >= 'a' && c <= 'z' ||
			c >= 'A' && c <= 'Z' ||
			c >= '0' && c <= '9' ||
			c == '_' ||
			c == '.' ||
			c == '-') {
			return ""
		}
	}
	return input
}

/* hash the password with the given salt using PBKDF2 */
func hashPassword(password, salt string) string {
	saltBytes, _ := hex.DecodeString(salt)
	hash := pbkdf2.Key([]byte(password), saltBytes, 10000, 64, sha512.New)
	return hex.EncodeToString(hash)
}
