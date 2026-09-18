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

    // Open database connection
    err = db.QueryRow("SELECT user_password FROM users WHERE user_id = ?", userID).StringScan(&storedHash)
    if err == sql.ErrNoRows {
        // User not found or no password stored
        w.WriteHeader(http.StatusUnauthorized)
        fmt.Fprintf(w, "Invalid user_id or current password")
        return
    } else if err != nil {
        // Database error
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Database error: %v", err)
        return
    }

    // Check if current password matches
    currentHash := sha256.Sum256([]byte(currentPassword))
    if currentHash.String != storedHash {
        w.WriteHeader(http.StatusUnauthorized)
        fmt.Fprintf(w, "Incorrect current password")
        return
    }

    // Hash the new password
    newHash := sha256.Sum256([]byte(newPassword))

    // Update password in database
    stmt, err := db.Prepare("UPDATE users SET user_password = ? WHERE user_id = ?")
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Database error: %v", err)
        return
    }
    defer stmt.Close()

    rowsAffected, err := stmt.Exec(newHash.String, userID)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Database error: %v", err)
        return
    }

    if rowsAffected.RowsAffected() != 1 {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "User not found")
        return
    }

    // Return success response
    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "Password changed successfully")
}