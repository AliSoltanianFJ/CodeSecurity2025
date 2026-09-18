package main

import (
    "crypto/sha256"
    "encoding/json"
    "fmt"
    "net/http"

    "database/sql"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func changePassword(w http.ResponseWriter, r *http.Request) {
    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    // Check if all required fields are present
    if userID == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "{ \"error\": \"user_id is required\" }")
        return
    }
    if currentPassword == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "{ \"error\": \"current_password is required\" }")
        return
    }
    if newPassword == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "{ \"error\": \"new_password is required\" }")
        return
    }

    // Function to compute SHA256 hash of a string
    hashPassword := func(password string) string {
        h := sha256.New([]byte(password))
        return fmt.Sprintf("%x", h.Sum(nil))
    }

    // Verify current password
    query := "SELECT password_hash FROM users WHERE user_id = ?"
    stmt, err := db.Prepare(query)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "{ \"error\": \"Database error\" }")
        return
    }
    defer stmt.Close()

    var currentHash string
    err = stmt.QueryRow(userID).Scan(&currentHash)
    if err != nil {
        if err == sql.ErrNotFound {
            w.WriteHeader(http.StatusNotFound)
            fmt.Fprintf(w, "{ \"error\": \"User not found\" }")
            return
        }
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "{ \"error\": \"Database error\" }")
        return
    }

    if hashPassword(currentPassword) != currentHash {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "{ \"error\": \"Current password is incorrect\" }")
        return
    }

    // Update new password
    queryUpdate := "UPDATE users SET password_hash = ? WHERE user_id = ?"
    stmtUpdate, err := db.Prepare(queryUpdate)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "{ \"error\": \"Database error\" }")
        return
    }
    defer stmtUpdate.Close()

    _, err = stmtUpdate.Exec(hashPassword(newPassword), userID)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "{ \"error\": \"Failed to update password\" }")
        return
    }

    // Return success response
    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "{ \"message\": \"Password updated successfully\" }")
}