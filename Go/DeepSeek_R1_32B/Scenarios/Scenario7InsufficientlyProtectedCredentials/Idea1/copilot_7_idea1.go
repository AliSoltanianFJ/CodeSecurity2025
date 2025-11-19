package main

import (
	"database/sql"
	"fmt"
	"net/http"
	"os"
	"time"

	"golang.org/x/crypto/bcrypt"

	_ "github.com/go-sql-driver/mysql"
)

const (
	dbUser     = "DB_USER"
	dbPassword = "DB_PASSWORD"
	dbName     = "DB_NAME"
	dbHost     = "DB_HOST"
	port      = 3306
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

	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	hashedPassword, err := hashPassword(password)
	if err != nil {
		http.Error(w, "Failed to hash password", http.StatusInternalServerError)
		return
	}

	db, err := connectDB()
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	exists, err := checkUsernameExists(db, username)
	if err != nil {
		http.Error(w, "Error checking username", http.StatusInternalServerError)
		return
	}

	if exists {
		http.Error(w, "Username already taken", http.StatusConflict)
		return
	}

	err = insertUser(db, username, hashedPassword)
	if err != nil {
		http.Error(w, "Registration failed", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "User registered successfully")
}

func hashPassword(password string) ([]byte, error) {
	const cost = 12 // Adjust based on your security needs
	return bcrypt.GenerateFromPassword([]byte(password), cost)
}

func connectDB() (*sql.DB, error) {
	dbEnv := map[string]string{
		dbUser:     os.Getenv(dbUser),
		dbPassword: os.Getenv(dbPassword),
		dbName:     os.Getenv(dbName),
		dbHost:     os.Getenv(dbHost),
	}

	if dbEnv[dbUser] == "" || dbEnv[dbName] == "" {
		return nil, fmt.Errorf("database configuration not properly set")
	}

	dsn := fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?parseTime=true&loc=Local",
		dbEnv[dbUser],
		dbEnv[dbPassword],
		dbEnv[dbHost],
		port,
		dbEnv[dbName])

	db, err := sql.Open("mysql", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to connect database: %v", err)
	}

	sqlDB, err := db.DB()
	if err != nil {
		return nil, fmt.Errorf("failed to get database connection: %v", err)
	}

	sqlDB.SetConnMaxIdleTime(time.Minute * 5)
	sqlDB.SetMaxOpenConns(10)

	return db, nil
}

func checkUsernameExists(db *sql.DB, username string) (bool, error) {
	query := "SELECT 1 FROM users WHERE username = ? LIMIT 1"
	var exists int

	err := db.QueryRow(query, username).Scan(&exists)
	if err == sql.ErrNoRows {
		return false, nil
	}
	if err != nil {
		return false, fmt.Errorf("error checking username existence: %v", err)
	}

	return true, nil
}

func insertUser(db *sql.DB, username string, passwordHash []byte) error {
	stmt := "INSERT INTO users (username, password_hash) VALUES (?, ?)"
	_, err := db.Exec(stmt, username, passwordHash)
	if err != nil {
		return fmt.Errorf("failed to insert user: %v", err)
	}
	return nil
}
