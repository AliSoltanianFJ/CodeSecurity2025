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
    passwordMinLength = 12
    passwordMaxLength = 64
    usernameRegex     = `^[a-zA-Z0-9_]{3,30}$` // Allow alphanumeric and underscores, 3-30 characters
)

var (
    db *sql.DB
)

func init() {
    // Securely initialize the database connection
    var err error
    db, err = sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
    if err != nil {
        panic("Failed to connect to the database")
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

    // Parse and sanitize form inputs
    username := html.EscapeString(r.FormValue("username"))
    password := r.FormValue("password")

    // Validate inputs
    if err := validateUsername(username); err != nil {
        http.Error(w, "Invalid username", http.StatusBadRequest)
        return
    }
    if err := validatePassword(password); err != nil {
        http.Error(w, "Invalid password", http.StatusBadRequest)
        return
    }

    // Hash the password with a unique salt
    hashedPassword, err := hashPassword(password)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Store the user securely in the database
    if err := storeUser(username, hashedPassword); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // Respond to the client
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

func validateUsername(username string) error {
    re := regexp.MustCompile(usernameRegex)
    if !re.MatchString(username) {
        return errors.New("username does not meet requirements")
    }
    return nil
}

func validatePassword(password string) error {
    if len(password) < passwordMinLength || len(password) > passwordMaxLength {
        return errors.New("password length is invalid")
    }
    return nil
}

func hashPassword(password string) (string, error) {
    // Generate a random salt
    salt := make([]byte, 16)
    if _, err := rand.Read(salt); err != nil {
        return "", err
    }

    // Hash the password with the salt using SHA-256
    hash := sha256.New()
    hash.Write(salt)
    hash.Write([]byte(password))
    hashedPassword := hash.Sum(nil)

    // Encode the salt and hash as a single string
    return base64.StdEncoding.EncodeToString(salt) + "$" + base64.StdEncoding.EncodeToString(hashedPassword), nil
}

func storeUser(username, hashedPassword string) error {
    // Use parameterized queries to prevent SQL injection
    stmt, err := db.Prepare("INSERT INTO users (username, password) VALUES (?, ?)")
    if err != nil {
        return err
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, hashedPassword)
    return err
}
