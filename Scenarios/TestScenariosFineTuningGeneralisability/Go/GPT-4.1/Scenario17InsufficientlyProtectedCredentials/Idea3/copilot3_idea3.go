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

    // Input validation (basic example)
    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing required fields", http.StatusBadRequest)
        return
    }

    // Retrieve the current password hash from the database
    var storedHash string
    err := db.QueryRow("SELECT password_hash FROM users WHERE user_id = ?", userID).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "User not found", http.StatusNotFound)
        return
    } else if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // Compare the current password with the stored hash
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(currentPassword)); err != nil {
        http.Error(w, "Current password is incorrect", http.StatusUnauthorized)
        return
    }

    // Hash the new password using bcrypt
    newHash, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Failed to hash new password", http.StatusInternalServerError)
        return
    }

    // Update the password hash in the database
    _, err = db.Exec("UPDATE users SET password_hash = ? WHERE user_id = ?", newHash, userID)
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "Password updated successfully")
}
