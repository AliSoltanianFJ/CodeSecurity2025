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

    // 1. Load the user's existing password hash
    var storedHash string
    err := db.QueryRow("SELECT password_hash FROM users WHERE id = ?", userID).
        Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "user not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // 2. Verify current password
    curSum := sha256.Sum256([]byte(currentPassword))
    if fmt.Sprintf("%x", curSum) != storedHash {
        http.Error(w, "current password is incorrect", http.StatusUnauthorized)
        return
    }

    // 3. Hash the new password
    newSum := sha256.Sum256([]byte(newPassword))
    newHash := fmt.Sprintf("%x", newSum)

    // 4. Update in the database
    _, err = db.Exec(
        "UPDATE users SET password_hash = ? WHERE id = ?",
        newHash, userID,
    )
    if err != nil {
        http.Error(w, "failed to update password", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("password updated successfully"))
}
