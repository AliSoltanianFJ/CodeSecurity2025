package main

import (
    "crypto/sha256"
    "database/sql"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    var dbErr error
    db, dbErr = sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
    if dbErr != nil {
        panic(dbErr)
    }
    http.HandleFunc("/change-password", changePasswordHandler)
    http.ListenAndServe(":8080", nil)
}

func changePasswordHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing parameters", http.StatusBadRequest)
        return
    }

    var storedSalt string
    err := db.QueryRow("SELECT password_salt FROM users WHERE id = ?", userID).Scan(&storedSalt)
    if err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    hashedCurrentPassword, err := bcrypt.GenerateFromPassword([]byte(currentPassword), 12)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    randomSalt, err := bcrypt.GenerateRandomSalt(16)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    hashedNewPassword := bcrypt.HashPassword(randomSalt, newPassword)

    _, err = db.Exec("UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?", hashedNewPassword, randomSalt, userID)
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Password updated successfully.")
}