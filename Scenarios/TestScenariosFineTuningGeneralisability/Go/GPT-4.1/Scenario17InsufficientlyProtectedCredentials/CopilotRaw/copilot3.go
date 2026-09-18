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

    // Get the user's current password hash
    var storedHash string
    err := db.QueryRow("SELECT password FROM users WHERE id = ?", userID).Scan(&storedHash)
    if err != nil {
        http.Error(w, "User not found", http.StatusNotFound)
        return
    }

    // Compare the current password
    currentHash := fmt.Sprintf("%x", sha256.Sum256([]byte(currentPassword)))
    if storedHash != currentHash {
        http.Error(w, "Current password is incorrect", http.StatusUnauthorized)
        return
    }

    // Hash the new password
    newHash := fmt.Sprintf("%x", sha256.Sum256([]byte(newPassword)))

    // Update the password in the database
    _, err = db.Exec("UPDATE users SET password = ? WHERE id = ?", newHash, userID)
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "Password changed successfully")
}
