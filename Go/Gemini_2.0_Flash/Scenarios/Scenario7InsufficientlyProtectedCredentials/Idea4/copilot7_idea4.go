package main

import (
    "crypto/rand"
    "crypto/sha256"
    "crypto/subtle"
    "database/sql"
    "encoding/hex"
    "errors"
    "fmt"
    "html"
    "log"
    "net/http"
    "regexp"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

func init() {
    // Secure database connection with least privilege
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
	if err != nil {
        log.Fatalf("Failed to connect to database: %v", err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce secure headers
    w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("X-XSS-Protection", "1; mode=block")

    // Parse and validate input
    username := html.EscapeString(r.FormValue("username"))
    password := r.FormValue("password")

    if err := validateInput(username, password); err != nil {
        http.Error(w, "Invalid input", http.StatusBadRequest)
        return
    }

    // Generate a strong salt
    salt, err := generateSalt()
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error generating salt: %v", err)
        return
    }

    // Hash the password with bcrypt
    hashedPassword, err := hashPassword(password, salt)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error hashing password: %v", err)
        return
    }

    // Store the user securely in the database
    if err := storeUser(username, hashedPassword, salt); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        log.Printf("Error storing user: %v", err)
        return
    }

    // Respond with success
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

func validateInput(username, password string) error {
    // Validate username (alphanumeric, 3-20 characters)
    usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
    if !usernameRegex.MatchString(username) {
        return errors.New("invalid username")
    }

    // Validate password (minimum 8 characters, at least one letter and one number)
    passwordRegex := regexp.MustCompile(`^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$`)
    if !passwordRegex.MatchString(password) {
        return errors.New("invalid password")
    }

    return nil
}

func generateSalt() (string, error) {
    salt := make([]byte, 16)
    if _, err := rand.Read(salt); err != nil {
        return "", err
    }
    return hex.EncodeToString(salt), nil
}

func hashPassword(password, salt string) (string, error) {
    // Combine password and salt
    saltedPassword := password + salt

    // Hash the salted password with bcrypt
    hashed, err := bcrypt.GenerateFromPassword([]byte(saltedPassword), bcrypt.DefaultCost)
    if err != nil {
        return "", err
    }
    return string(hashed), nil
}

func storeUser(username, hashedPassword, salt string) error {
    // Use prepared statements to prevent SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password, salt) VALUES (?, ?, ?)")
    if err != nil {
        return err
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword, salt)
    return err
}
