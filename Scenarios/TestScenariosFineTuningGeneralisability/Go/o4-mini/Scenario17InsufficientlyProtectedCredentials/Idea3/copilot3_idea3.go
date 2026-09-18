package main

import (
    "database/sql"
    "errors"
    "fmt"
    "net/http"
    "unicode"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

// call this early in main()
func initDB(dataSourceName string) error {
    var err error
    db, err = sql.Open("mysql", dataSourceName)
    if err != nil {
        return err
    }
    // optional: tune MaxOpenConns, MaxIdleConns, ConnMaxLifetime
    return db.Ping()
}

func changePassword(w http.ResponseWriter, r *http.Request) {
    // 1) Parse form
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }
    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    // 2) Load the user's current password hash from the database
    var storedHash []byte
    err := db.QueryRow(
        "SELECT password_hash FROM users WHERE id = ?",
        userID,
    ).Scan(&storedHash)
    if err != nil {
        if err == sql.ErrNoRows {
            // generic error so we don’t reveal “user does not exist”
            http.Error(w, "Invalid credentials", http.StatusUnauthorized)
            return
        }
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // 3) Compare the supplied current password with the stored hash
    if err := bcrypt.CompareHashAndPassword(storedHash, []byte(currentPassword)); err != nil {
        // wrong password (or stored hash was malformed) – still generic
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // 4) (Optional) Enforce your password policy on the new password
    if err := validateNewPassword(newPassword); err != nil {
        http.Error(w, fmt.Sprintf("Invalid new password: %v", err), http.StatusBadRequest)
        return
    }

    // 5) Hash the new password with bcrypt
    newHash, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // 6) Store the new hash
    _, err = db.Exec(
        "UPDATE users SET password_hash = ? WHERE id = ?",
        newHash, userID,
    )
    if err != nil {
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // 7) Return success
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Password changed"))
}

// validateNewPassword enforces whatever policy you like.
// Here we do a few simple checks: length >= 8, at least one digit, one letter.
func validateNewPassword(pw string) error {
    if len(pw) < 8 {
        return errors.New("must be at least 8 characters long")
    }
    var hasLetter, hasDigit bool
    for _, r := range pw {
        switch {
        case unicode.IsLetter(r):
            hasLetter = true
        case unicode.IsDigit(r):
            hasDigit = true
        }
    }
    if !hasLetter || !hasDigit {
        return errors.New("must contain at least one letter and one digit")
    }
    return nil
}