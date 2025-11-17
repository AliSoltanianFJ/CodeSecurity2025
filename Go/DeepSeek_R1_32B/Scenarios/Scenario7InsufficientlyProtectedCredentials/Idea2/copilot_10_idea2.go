package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"os"
	"strings"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/register", registerHandler)
	http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := strings.TrimSpace(r.FormValue("username"))
	password := strings.TrimSpace(r.FormValue("password"))

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	if len(username) < 3 || len(password) < 6 {
		http.Error(w, "Username must be at least 3 characters and password at least 6", http.StatusBadRequest)
		return
	}

	hashedPassword := sha256.Sum256([]byte(password))
	passwordHash := make([]byte, hex.EncodedLen(len(hashedPassword)))
	hex.Encode(passwordHash, hashedPassword[:])

	dbHost := os.Getenv("DB_HOST")
	dbUser := os.Getenv("DB_USER")
	dbPass := os.Getenv("DB_PASS")
	dbName := os.Getenv("DB_NAME")

	if dbHost == "" || dbUser == "" || dbPass == "" || dbName == "" {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	connStr := fmt.Sprintf("%s:%s@tcp(%s)/%s?charset=utf8mb4&parseTime=True&loc=Local",
		dbUser,
		dbPass,
		dbHost,
		dbName)

	db, err := sql.Open("mysql", connStr)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	_, err = stmt.Exec(username, string(passwordHash))
	if err != nil {
		if strings.HasPrefix(err.Error(), "Error 1062: ") {
			http.Error(w, "Username already exists", http.StatusConflict)
		} else {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}

	fmt.Fprintf(w, "Registration successful\n")
}
