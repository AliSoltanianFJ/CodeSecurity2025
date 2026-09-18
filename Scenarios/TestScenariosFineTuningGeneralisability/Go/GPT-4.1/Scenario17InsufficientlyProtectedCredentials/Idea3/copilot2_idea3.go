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

    // Input validation
    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing required fields", http.StatusBadRequest)
        return
    }

    // Fetch current password hash from DB
    var storedHash string
    err := db.QueryRow("SELECT password_hash FROM users WHERE id = ?", userID).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "User not found", http.StatusNotFound)
        return
    } else if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Compare current password
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(currentPassword)); err != nil {
        http.Error(w, "Current password is incorrect", http.StatusUnauthorized)
        return
    }

    // Hash new password using bcrypt
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Error hashing password", http.StatusInternalServerError)
        return
    }

    // Update password in DB
    _, err = db.Exec("UPDATE users SET password_hash = ? WHERE id = ?", hashedPassword, userID)
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "Password updated successfully")
}