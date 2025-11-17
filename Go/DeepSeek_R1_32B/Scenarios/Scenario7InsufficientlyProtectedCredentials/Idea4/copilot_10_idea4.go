package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"time"

	"github.com/go-sql-driver/mysql"
)

const (
	DBHost     = "localhost:3306"
	DBName     = "mydb"
	DBUser     = "root"
	DBPassword = "password"
)

var db *sql.DB

func initDB() error {
	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@tcp(%s)/%s?parseTime=true", DBUser, DBPassword, DBHost, DBName))
	if err != nil {
		return err
	}
	db.SetConnMaxLifetime(time.Hour)
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)
	return nil
}

func init() {
	err := initDB()
	if err != nil {
		panic(fmt.Sprintf("Failed to initialize database: %v", err))
	}
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if len(username) == 0 || len(password) < 8 {
		http.Error(w, "Invalid username or password length", http.StatusBadRequest)
		return
	}

	hashedPassword, err := hashPassword(password)
	if err != nil {
		http.Error(w, "Failed to hash password", http.StatusInternalServerError)
		return
	}

	usernameSanitized := sql.EscapeIdentifier(db.Driver(), username)

	query := `SELECT COUNT(*) FROM users WHERE username = ?`
	var count int
	if err := db.QueryRow(query, usernameSanitized).Scan(&count); err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	if count > 0 {
		http.Error(w, "Username already exists", http.StatusConflict)
		return
	}

	query = `INSERT INTO users (username, password_hash) VALUES (?, ?)`
	stmt, err := db.Prepare(query)
	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	result, err := stmt.Exec(usernameSanitized, hashedPassword)
	if err != nil {
		if _, ok := err.(*mysql.MySQLError); ok && err.Number == 1062 {
			http.Error(w, "Username already exists", http.StatusConflict)
			return
		}
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	id, err := result.LastInsertId()
	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Registration successful. User ID: %d", id)
}

func hashPassword(password string) ([]byte, error) {
	hasher := sha256.New()
	if _, err := hasher.Write([]byte(password)); err != nil {
		return nil, fmt.Errorf("failed to write password to hasher: %v", err)
	}
	return hasher.Sum(nil), nil
}

func main() {
	http.HandleFunc("/register", registerHandler)
	http.ListenAndServe(":8080", nil)
}
