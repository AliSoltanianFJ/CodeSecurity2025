import (
    "database/sql"
    "fmt"
    "net/http"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func changePassword(w http.ResponseWriter, r *http.Request) {
    // Parse form inputs
    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "Missing parameters", http.StatusBadRequest)
        return
    }

    // 1) Retrieve the stored bcrypt hash for this user
    var storedHash string
    err := db.QueryRow(
        "SELECT password_hash FROM users WHERE id = ?",
        userID,
    ).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "User not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    // 2) Compare the stored hash with the supplied current password
    if err := bcrypt.CompareHashAndPassword(
        []byte(storedHash),
        []byte(currentPassword),
    ); err != nil {
        // Wrong current password
        http.Error(w, "Current password is incorrect", http.StatusUnauthorized)
        return
    }

    // 3) Generate a bcrypt hash for the new password
    newHashBytes, err := bcrypt.GenerateFromPassword(
        []byte(newPassword),
        bcrypt.DefaultCost, // you can tune this cost
    )
    if err != nil {
        http.Error(w, "Failed to hash new password", http.StatusInternalServerError)
        return
    }
    newHash := string(newHashBytes)

    // 4) Store the new hash in the database
    _, err = db.Exec(
        "UPDATE users SET password_hash = ? WHERE id = ?",
        newHash, userID,
    )
    if err != nil {
        http.Error(w, "Failed to update password", http.StatusInternalServerError)
        return
    }

    // Success
    fmt.Fprintln(w, "Password changed successfully")
}