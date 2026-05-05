package main

import (
    "crypto/rand"
    "crypto/sha256"
    "database/sql"
    "encoding/base64"
    "errors"
    "fmt"
    "html"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

const (
    passwordHashAlgorithm = "SHA256" // Documenting the algorithm used
    saltLength            = 16       // Length of the salt in bytes
)

var (
    db *sql.DB // Database connection pool
)

// Initialize the database connection securely
func init() {
    var err error
    // Use environment variables or a secure secrets manager to retrieve credentials
    dsn := getSecureDSN()
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        panic("Failed to connect to the database securely")
    }
    // Set secure database connection settings
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
}

// Securely retrieve the database DSN (Data Source Name)
func getSecureDSN() string {
    // Replace this with a secure secrets management solution
    return "user:password@tcp(localhost:3306)/securedb"
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and validate form inputs
    err := r.ParseForm()
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Validate username and password
    if !isValidUsername(username) {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if !isValidPassword(password) {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Hash the password securely
    hashedPassword, err := hashPassword(password)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Store the user securely in the database
    err = storeUser(username, hashedPassword)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Respond with a success message
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

// Validate the username (allow only alphanumeric and 3-20 characters)
func isValidUsername(username string) bool {
    usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
    return usernameRegex.MatchString(username)
}

// Validate the password (minimum 8 characters, at least one letter, one number, and one special character)
func isValidPassword(password string) bool {
    passwordRegex := regexp.MustCompile(`^(?=.*[a-zA-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$`)
    return passwordRegex.MatchString(password)
}

// Hash the password securely using SHA-256 and a random salt
func hashPassword(password string) (string, error) {
    salt := make([]byte, saltLength)
    _, err := rand.Read(salt)
    if err != nil {
        return "", errors.New("failed to generate salt")
    }

    saltedPassword := append(salt, []byte(password)...)
	hash := sha256.Sum256(saltedPassword)

    // Encode the salt and hash as a single string
    return base64.StdEncoding.EncodeToString(append(salt, hash[:]...)), nil
}

// Store the user securely in the database
func storeUser(username, hashedPassword string) error {
    // Use parameterized queries to prevent SQL injection
    query := "INSERT INTO users (username, password_hash) VALUES (?, ?)"
    _, err := db.Exec(query, html.EscapeString(username), hashedPassword)
    return err
}
