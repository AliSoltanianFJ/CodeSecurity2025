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
    var salt []byte
    err := db.QueryRow("SELECT salt FROM users WHERE id = ?", userID).Scan(&salt)
    if err != sql.ErrNoRows && err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    // If no user found (including sql.ErrNoRows), return a generic error
    if len(salt) == 0 {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Verify the current password (hashed with salt)
    currentHashed := sha256.Sum256(append([]byte(currentPassword), salt...))
    var storedHash []byte
    err = db.QueryRow("SELECT password_hash FROM user_passwords WHERE user_id = ? AND deleted = 0", userID).Scan(&storedHash)
    if err != nil || !bytes.Equal(currentHashed[:], storedHash) {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Soft delete the old password record (for audit purposes)
    _, err = db.Exec("UPDATE user_passwords SET deleted = 1 WHERE user_id = ? AND deleted = 0", userID)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Store the new password (hashed with salt)
    newHashed := sha256.Sum256(append([]byte(newPassword), salt...))
    _, err = db.Exec("INSERT INTO user_passwords (user_id, password_hash, created_at) VALUES (?, ?, ?)", userID, newHashed[:], time.Now().UTC())
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Password updated successfully")
}

