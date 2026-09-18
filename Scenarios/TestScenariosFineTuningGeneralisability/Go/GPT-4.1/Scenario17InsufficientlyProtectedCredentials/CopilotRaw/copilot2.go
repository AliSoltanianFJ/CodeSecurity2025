import (
    "crypto/sha256"
    "database/sql"
    "encoding/hex"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func hashPassword(pw string) string {
    hash := sha256.Sum256([]byte(pw))
    return hex.EncodeToString(hash[:])
}

func changePassword(w http.ResponseWriter, r *http.Request) {
    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    // Get current hashed password from database
    var storedHash string
    err := db.QueryRow("SELECT password FROM users WHERE id = ?", userID).Scan(&storedHash)
    if err != nil {
        http.Error(w, "User not found", http.StatusBadRequest)
        return
    }

    // Compare hashes of currentPassword and stored password
    if storedHash != hashPassword(currentPassword) {
        http.Error(w, "Current password is incorrect", http.StatusUnauthorized)
        return
    }

    // Hash the new password
    newHashed := hashPassword(newPassword)

    // Update the user's password in the database
    _, err = db.Exec("UPDATE users SET password = ? WHERE id = ?", newHashed, userID)
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    fmt.Fprint(w, "Password changed successfully")
}
