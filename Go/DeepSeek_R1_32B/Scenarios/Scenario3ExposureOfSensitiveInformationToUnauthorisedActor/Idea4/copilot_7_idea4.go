package main

import (
	"encoding/gob"
	"regexp"
	"fmt"
	"net/http"
	"os"
	"strconv"
	"database/sql"
	"time"

	"github.com/dgrijalva/jwt-go"
	"golang.org/x/crypto/bcrypt"

	_ "github.com/go-sql-driver/mysql"
)


var (
	db             *sql.DB
	sessionTimeout time.Duration = 3600
	jwtSecret      []byte
)


func main() {
	if err := setup(); err != nil {
		return
	}

	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if len(username) == 0 || len(password) == 0 {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if !isSafeInput(username) || !isSafeInput(password) {
		http.Error(w, "Invalid characters in input", http.StatusBadRequest)
		return
	}

	var hashedPassword string
	err := db.QueryRow("SELECT password_hash FROM users WHERE username=?", sanitizeUsername(username)).Scan(&hashedPassword)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Invalid credentials", http.StatusUnauthorized)
			return
		}
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"username": username,
		"exp":      time.Now().Add(sessionTimeout).Unix(),
	})

	tokenString, err := token.SignedString(jwtSecret)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     "auth_token",
		Value:    tokenString,
		Expires:  time.Now().Add(sessionTimeout),
		HttpOnly: true,
		Secure:   true,
	})
	w.WriteHeader(http.StatusOK)
}

// Setup initial configuration and database connection
func setup() error {
	gob.Register(time.Time{})

	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@tcp(%s:%d)/%s",
		os.Getenv("DB_USER"),
		os.Getenv("DB_PASSWORD"),
		os.Getenv("DB_HOST"),
		getEnvInt("DB_PORT", 3306),
		os.Getenv("DB_NAME")))
	if err != nil {
		return fmt.Errorf("failed to connect to database: %v", err)
	}
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)

	jwtSecret = []byte(os.Getenv("JWT_SECRET"))
	if len(jwtSecret) == 0 {
		return fmt.Errorf("JWT secret must be set")
	}

	return nil
}

func getEnvInt(key string, defaultValue int) int {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}
	i, err := strconv.Atoi(value)
	if err != nil {
		return defaultValue
	}
	return i
}

func isSafeInput(input string) bool {
	safeChars := regexp.MustCompile("^[a-zA-Z0-9_@.-]+$")
	return safeChars.MatchString(input[:50])
}

func sanitizeUsername(username string) string {
	if len(username) > 50 {
		username = username[:50]
	}
	return regexp.MustCompile(`[^a-zA-Z0-9_]`).ReplaceAllLiteralString(username, "")
}
