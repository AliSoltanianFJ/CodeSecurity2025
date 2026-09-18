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

    // Input validation
    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing required fields", http.StatusBadRequest)
        return
    }

    // Fetch user's current password hash from the database
    var storedHash string
    err := db.QueryRow("SELECT password_hash FROM users WHERE user_id = ?", userID).Scan(&storedHash)
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

    // Validate new password strength (basic example)
    if len(newPassword) < 8 {
        http.Error(w, "New password must be at least 8 characters", http.StatusBadRequest)
        return
    }

    // Hash the new password using bcrypt
    hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "Error processing password", http.StatusInternalServerError)
        return
    }

    // Update the password in the database using a prepared statement
    stmt, err := db.Prepare("UPDATE users SET password_hash = ? WHERE user_id = ?")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(hashedPassword, userID)
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Password changed successfully")
}
