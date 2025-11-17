package main

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"html/template"
	"log"
	"net/http"
	"os"
	"regexp"
	"strconv"
	"time"

	"golang.org/x/crypto/bcrypt"
	"github.com/mattn/go-sqlite3"
)

const (
	envBcryptCost      = "BCRYPT_COST"
	envDBPath         = "DATABASE_PATH"
	defaultBcryptCost  = 12
	defaultDBPath     = "./mod_auth.db"
	usernameRegexStr   = `^[a-zA-Z0-9_]+$`
	passwordMinLength = 8
	passwordMaxLength = 64
)

var (
	db *sql.DB

	usernameRegex = regexp.MustCompile(usernameRegexStr)
)

func init() {
	var err error
	bcryptCost := defaultBcryptCost
	if os.Getenv(envBcryptCost) != "" {
		bcryptCost, err = strconv.Atoi(os.Getenv(envBcryptCost))
		if err != nil {
			log.Printf("Using default bcrypt cost: %d", defaultBcryptCost)
			bcryptCost = defaultBcryptCost
		}
	}

	dbPath := os.Getenv(envDBPath)
	if dbPath == "" {
		dbPath = defaultDBPath
	}

	db, err = sql.Open("sqlite3", dbPath)
	if err != nil {
		log.Fatal(err)
	}

	setupScript := `
	CREATE TABLE IF NOT EXISTS users (
		username TEXT PRIMARY KEY,
		password_hash TEXT NOT NULL,
		role TEXT NOT NULL
	);
	INSERT OR IGNORE INTO users (username, password_hash, role) VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'moderator');
	INSERT OR IGNORE INTO users (username, password_hash, role) VALUES ('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'user');
	`
	if _, err := db.Exec(setupScript); err != nil {
		log.Fatal(err)
	}
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*5)
	defer cancel()

	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	if username == "" || password == "" {
		http.Error(w, "Missing required parameters", http.StatusBadRequest)
		return
	}

	if !usernameRegex.MatchString(username) {
		http.Error(w, "Invalid username format", http.StatusBadRequest)
		return
	}

	if len(password) < passwordMinLength || len(password) > passwordMaxLength {
		http.Error(w, "Invalid password length", http.StatusBadRequest)
		return
	}

	var hashedPassword string
	err := db.QueryRowContext(ctx, "SELECT password_hash FROM users WHERE username=?", username).Scan(&hashedPassword)
	switch {
	case errors.Is(err, sql.ErrNoRows):
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	case err != nil:
		log.Printf("Database error: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	var role string
	err = db.QueryRowContext(ctx, "SELECT role FROM users WHERE username=?", username).Scan(&role)
	if err != nil {
		log.Printf("Database error: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if role == "moderator" {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "true")
	} else {
		w.WriteHeader(http.StatusForbidden)
		fmt.Fprintf(w, "false")
	}
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	log.Printf("Starting server on :8080 with database at %s\n", db.DBName())
	if err := http.ListenAndServe(":8080", nil); err != nil {
		log.Fatal(err)
	}
}
