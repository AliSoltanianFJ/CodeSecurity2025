package main

import (
    "database/sql"
    "errors"
    "fmt"
    "net/http"
    "unicode/utf8"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

var db *sql.DB

// changePassword is an HTTP handler that:
// 1) verifies the user's current password,
// 2) validates + re-hashes the new password,
// 3) updates the password hash in the database.
func changePassword(w http.ResponseWriter, r *http.Request) {
    // 0) You probably want to pull userID from a session or JWT,
    //    not directly from the form.  Here we'll assume you
    //    have middleware that sets it in the context:
    userID, ok := r.Context().Value("userID").(int64)
    if !ok {
        http.Error(w, "authentication required", http.StatusUnauthorized)
        return
    }

    // 1) Parse form
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form data", http.StatusBadRequest)
        return
    }
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    // 2) Basic new-password policy (e.g. minimum length)
    const minPasswordLength = 8
    if utf8.RuneCountInString(newPassword) < minPasswordLength {
        http.Error(w, fmt.Sprintf("new password must be at least %d characters", minPasswordLength), http.StatusBadRequest)
        return
    }

    // 3) Retrieve the user's current bcrypt hash from the database
    var existingHash string
    err := db.QueryRow(`
        SELECT password_hash
          FROM users
         WHERE id = ?
    `, userID).Scan(&existingHash)
    if err == sql.ErrNoRows {
        // user not found in DB
        http.Error(w, "invalid credentials", http.StatusUnauthorized)
        return
    } else if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // 4) Compare supplied current password against stored hash
    if err := bcrypt.CompareHashAndPassword([]byte(existingHash), []byte(currentPassword)); err != nil {
        // wrong password (CompareHashAndPassword runs in constant time)
        http.Error(w, "current password is incorrect", http.StatusUnauthorized)
        return
    }

    // 5) Hash the new password with bcrypt (this adds its own salt & cost)
    const bcryptCost = bcrypt.DefaultCost
    newHashBytes, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcryptCost)
    if err != nil {
        // extremely unlikely, but handle it
        http.Error(w, "failed to hash password", http.StatusInternalServerError)
        return
    }
    newHash := string(newHashBytes)

    // 6) Update the database with the new hash
    //    Use a prepared statement or parameterized query to avoid injection
    res, err := db.Exec(`
        UPDATE users
           SET password_hash = ?
         WHERE id = ?
    `, newHash, userID)
    if err != nil {
        http.Error(w, "failed to update password", http.StatusInternalServerError)
        return
    }
    rowsAffected, _ := res.RowsAffected()
    if rowsAffected != 1 {
        // This really should never happen unless the row was deleted
        http.Error(w, "failed to update password", http.StatusInternalServerError)
        return
    }

    // 7) Success
    w.WriteHeader(http.StatusOK)
    fmt.Fprintln(w, "password changed successfully")
}

// ------------------------------------------------
// Boilerplate to open your DB, set up your routes
// ------------------------------------------------
func main() {
    var err error
    // Adjust the DSN to your username:password@tcp(host:port)/dbname
    db, err = sql.Open("mysql", "user:pass@tcp(localhost:3306)/yourdb?parseTime=true")
    if err != nil {
        panic(err)
    }
    if err = db.Ping(); err != nil {
        panic(err)
    }

    http.HandleFunc("/change-password", changePassword)
    http.ListenAndServe(":8080", nil)
}