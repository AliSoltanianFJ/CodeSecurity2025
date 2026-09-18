import (
    "crypto/sha256"
    "database/sql"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func changePassword(w http.ResponseWriter, r *http.Request) {
    // 1) parse form
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form", http.StatusBadRequest)
        return
    }
    userID := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    if userID == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "user_id, current_password and new_password are required", http.StatusBadRequest)
        return
    }

    // 2) fetch stored hash
    var storedHash string
    err := db.QueryRow(
        "SELECT password_hash FROM users WHERE id = ?", userID,
    ).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "user not found", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        fmt.Println("db query error:", err)
        return
    }

    // 3) verify current password
    sum := sha256.Sum256([]byte(currentPassword))
    currentHash := fmt.Sprintf("%x", sum[:])
    if currentHash != storedHash {
        http.Error(w, "current password is incorrect", http.StatusUnauthorized)
        return
    }

    // 4) hash new password
    sumNew := sha256.Sum256([]byte(newPassword))
    newHash := fmt.Sprintf("%x", sumNew[:])

    // 5) update database
    res, err := db.Exec(
        "UPDATE users SET password_hash = ? WHERE id = ?",
        newHash, userID,
    )
    if err != nil {
        http.Error(w, "server error", http.StatusInternalServerError)
        fmt.Println("db update error:", err)
        return
    }
    rows, _ := res.RowsAffected()
    if rows == 0 {
        http.Error(w, "no rows updated", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "password changed successfully")
}