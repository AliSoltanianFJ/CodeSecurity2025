package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"
    "os"
    "regexp"

    "github.com/jmoiron/sqlx"
)

const (
    dbHost       = "localhost"
    dbPort       = 5432
    dbUser       = "postgres"
    dbName       = "newsletter"
    maxEmailSize = 254 // As per RFC 5321
)

var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// unsubscribe handles the unsubscription process for a given email
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")

    if err := validateEmail(email); err != nil {
        sendResponse(w, http.StatusBadRequest, "Invalid email format")
        return
    }

    db, err := sqlx.Open("postgres", os.Getenv("DATABASE_URL"))
    if err != nil {
        sendResponse(w, http.StatusInternalServerError, "Failed to connect to database")
        return
    }
    defer db.Close()

    var exists bool
    err = db.Get(&exists, "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email=$1)", email)
    if err != nil {
        sendResponse(w, http.StatusInternalServerError, "Database error occurred")
        return
    }

    if !exists {
        sendResponse(w, http.StatusNotFound, "Email not found in subscription list")
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email=$1", email)
    if err != nil {
        sendResponse(w, http.StatusInternalServerError, "Failed to unsubscribe")
        return
    }

    sendResponse(w, http.StatusOK, "Successfully unsubscribed")
}

// validateEmail checks if the provided email is valid
func validateEmail(email string) error {
    if len(email) > maxEmailSize {
        return fmt.Errorf("email too long")
    }
    if !emailRegex.MatchString(email) {
        return fmt.Errorf("invalid email format")
    }
    return nil
}

// sendResponse writes JSON response with status and message
func sendResponse(w http.ResponseWriter, status int, message string) {
    w.WriteHeader(status)
    if err := json.NewEncoder(w).Encode(map[string]string{"message": message}); err != nil {
        fmt.Fprint(w, `{"message":"internal server error"}`)
    }
}