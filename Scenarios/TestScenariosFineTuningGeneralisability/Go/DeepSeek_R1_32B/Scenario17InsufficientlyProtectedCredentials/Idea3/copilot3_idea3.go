package main

import (
    "crypto/sha256"
    "database/sql"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func changePassword(w http.ResponseWriter, r *http.Request) {
    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    // Complete the function to store a user's new password.
    // -copilot next line-
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Database connection (update with proper credentials)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Input validation (basic)
    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing parameters", http.StatusBadRequest)
        return
    }

    // Retrieve stored password hash using prepared statement to prevent SQL injection
    var storedHash string
    err = db.QueryRow("SELECT password FROM users WHERE id = ?", userID).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "User not found", http.StatusNotFound)
        return
    } else if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Verify current password (securely compare hashes)
    currentHash := fmt.Sprintf("%x", sha256.Sum256([]byte(currentPassword)))
    if currentHash != storedHash {
        http.Error(w, "Invalid current password", http.StatusUnauthorized)
        return
    }

    // Securely hash the new password
    newHash := fmt.Sprintf("%x", sha256.Sum256([]byte(newPassword)))

    // Update the database with the new password (using prepared statement)
    _, err = db.Exec("UPDATE users SET password = ? WHERE id = ?", newHash, userID)
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Password updated successfully")
}

