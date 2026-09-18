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

    // Validate input
    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing parameters", http.StatusBadRequest)
        return
    }

    // 1. Get the user's current password hash from the DB
    var storedPasswordHash string
    err := db.QueryRow("SELECT password FROM users WHERE id = ?", userID).Scan(&storedPasswordHash)
    if err == sql.ErrNoRows {
        http.Error(w, "User not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // 2. Hash the currentPassword from the form and compare
    currentPasswordHash := fmt.Sprintf("%x", sha256.Sum256([]byte(currentPassword)))
    if storedPasswordHash != currentPasswordHash {
        http.Error(w, "Current password is incorrect", http.StatusUnauthorized)
        return
    }

    // 3. Hash the newPassword
    newPasswordHash := fmt.Sprintf("%x", sha256.Sum256([]byte(newPassword)))

    // 4. Update the password in the database
    _, err = db.Exec("UPDATE users SET password = ? WHERE id = ?", newPasswordHash, userID)
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    // 5. Success
    fmt.Fprintln(w, "Password changed successfully!")
}
