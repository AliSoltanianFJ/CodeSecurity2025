package main

import (
    "database/sql"
    "encoding/hex"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func changePassword(w http.ResponseWriter, r *http.Request) {
    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    // Check for missing form values
    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing form values", 400)
        return
    }

    // Hash the current password
    currentHash := sha256.New()
    currentHash.Write([]byte(currentPassword))
    currentBytes := currentHash.Sum(nil)
    currentPassHash, err := hex.EncodeString(currentBytes)
    if err != nil {
        http.Error(w, "Error encoding current password hash", 500)
        return
    }

    // Hash the new password
    newHash := sha256.New()
    newHash.Write([]byte(newPassword))
    newBytes := newHash.Sum(nil)
    newPassHash, err := hex.EncodeString(newBytes)
    if err != nil {
        http.Error(w, "Error encoding new password hash", 500)
        return
    }

    // Check if current password is correct for the user
    checkStmt, err := db.Prepare("SELECT 1 FROM users WHERE user_id = ? AND password_hash = ?")
    if err != nil {
        http.Error(w, err.Error(), 500)
        return
    }
    defer checkStmt.Close()

    var found bool
    err = checkStmt.Query(userID, currentPassHash).Scan(&found)
    if err != nil {
        if err == sql.ErrNoRows {
            http.Error(w, "Incorrect current password or user not found", 400)
            return
        }
        http.Error(w, err.Error(), 500)
        return
    }

    // Update the password if current is valid
    _, err = db.Exec("UPDATE users SET password_hash = ? WHERE user_id = ? AND password_hash = ?", newPassHash, userID, currentPassHash)
    if err != nil {
        http.Error(w, "Failed to update password", 500)
        return
    }

    // Return success response
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Password changed successfully"))
}