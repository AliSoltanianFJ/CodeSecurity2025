package main

import (
	"context"
	"crypto/rand"
	"database/sql"
	"encoding/hex"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/joho/godotenv"
	"golang.org/x/crypto/bcrypt"
	"github.com/google/uuid"
	"github.com/gorilla/csrf"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	err := godotenv.Load()
	if err != nil {
		log.Println("Error loading .env file")
	}

	// CSRF protection setup
	csrfKey := make([]byte, 32)
	if _, err := rand.Read(csrfKey); err != nil {
		log.Fatal("Failed to generate CSRF key:", err)
	}

	// Initialize CSRF middleware with secure settings
	csrfMiddleware := csrf.Protect(csrfKey,
		csrf.Secure(true),       // Only allow secure connections (HTTPS)
		csrf.FieldName("csrfToken"), // Customize the CSRF field name
		csrf.HttpOnly(true),      // Mitigate XSS attacks by making the cookie inaccessible to JavaScript
		csrf.Path("/"),           // Set the cookie path
		csrf.MaxAge(86400),       // Set the maximum age of the token (24 hours)
	)

	http.HandleFunc("/login", loginHandler)

	// Wrap the handler with CSRF protection
	http.Handle("/login", csrfMiddleware(http.HandlerFunc(loginHandler)))

	log.Fatal(http.ListenAndServe(":8080", nil))
}

func generateSalt() (string, error) {
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		return "", err
	}
	return hex.EncodeToString(salt), nil
}

func hashPassword(password, salt string) (string, error) {
	hashed, err := bcrypt.GenerateFromPassword([]byte(password+salt), bcrypt.DefaultCost)
	if err != nil {
		return "", err
	}
	return string(hashed), nil
}

func isValidUsername(username string) bool {
	if len(username) < 3 || len(username) > 50 {
		return false
	}
	if strings.ContainsAny(username, "<>") {
		return false
	}
	return true
}

func isValidPassword(password string) bool {
	if len(password) < 8 {
		return false
	}
	return true
}

func sanitizeString(input string) string {
	escaped := strings.ReplaceAll(input, "<", "&lt;")
    escaped = strings.ReplaceAll(escaped, ">", "&gt;")
    escaped = strings.ReplaceAll(escaped, "\"", "&quot;")
    escaped = strings.ReplaceAll(escaped, "'", "&#39;")
    escaped = strings.ReplaceAll(escaped, "`", "&#96;")
	return escaped
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := sanitizeString(r.FormValue("username"))
	password := sanitizeString(r.FormValue("password"))

	if !isValidUsername(username) || !isValidPassword(password) {
		http.Error(w, "Invalid username or password", http.StatusBadRequest)
		return
	}

	dbUser := os.Getenv("DB_USER")
	dbPass := os.Getenv("DB_PASS")
	dbHost := os.Getenv("DB_HOST")
	dbName := os.Getenv("DB_NAME")

	connectionString := fmt.Sprintf("%s:%s@tcp(%s)/%s?tls=skip-verify", dbUser, dbPass, dbHost, dbName)
	db, err := sql.Open("mysql", connectionString)
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		log.Println("DB connection error:", err)
		return
	}
	defer db.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	var storedHash, storedSalt string
	query := "SELECT password, salt FROM users WHERE username = ?"

	stmt, err := db.PrepareContext(ctx, query)
	if err != nil {
		http.Error(w, "Failed to prepare statement", http.StatusInternalServerError)
		log.Println("Prepare error:", err)
		return
	}
	defer stmt.Close()

	err = stmt.QueryRowContext(ctx, username).Scan(&storedHash, &storedSalt)
	if err != nil {
		if err == sql.ErrNoRows {
			http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		} else {
			http.Error(w, "Database query failed", http.StatusInternalServerError)
			log.Println("Query error:", err)
		}
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password+storedSalt))
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	sessionToken := uuid.New().String()
    expiration := time.Now().Add(30 * time.Minute)

	setSessionQuery := "UPDATE users SET session_token = ?, session_expiry = ? WHERE username = ?"
    setSessionStmt, err := db.PrepareContext(ctx, setSessionQuery)
    if err != nil {
        http.Error(w, "Failed to prepare session statement", http.StatusInternalServerError)
        log.Println("Prepare session error:", err)
        return
    }
    defer setSessionStmt.Close()

    _, err = setSessionStmt.ExecContext(ctx, sessionToken, expiration, username)
    if err != nil {
        http.Error(w, "Failed to set session token", http.StatusInternalServerError)
        log.Println("Set session error:", err)
        return
    }

    cookie := &http.Cookie{
        Name:    "session_token",
        Value:   sessionToken,
        Expires: expiration,
		Secure:  true,
        HttpOnly: true,
        Path:    "/",
    }
    http.SetCookie(w, cookie)

	w.WriteHeader(http.StatusOK)
	w.Write([]byte("Login successful"))
}