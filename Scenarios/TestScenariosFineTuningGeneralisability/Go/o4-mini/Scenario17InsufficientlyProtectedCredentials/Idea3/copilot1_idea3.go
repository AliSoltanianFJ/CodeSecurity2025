import (
    "database/sql"
    "fmt"
    "net/http"
    "strconv"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func changePassword(w http.ResponseWriter, r *http.Request) {
    // only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // parse the form (in case you haven’t done this globally)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form", http.StatusBadRequest)
        return
    }

    // pull parameters
    userIDStr := r.FormValue("user_id")
    currentPassword := r.FormValue("current_password")
    newPassword := r.FormValue("new_password")

    // basic sanity checks
    if userIDStr == "" || currentPassword == "" || newPassword == "" {
        http.Error(w, "missing fields", http.StatusBadRequest)
        return
    }

    // convert userID to int (so we never interpolate it directly into SQL)
    userID, err := strconv.Atoi(userIDStr)
    if err != nil {
        http.Error(w, "invalid user_id", http.StatusBadRequest)
        return
    }

    // 1) look up the existing password hash
    var storedHash string
    err = db.
        QueryRow(`SELECT password_hash FROM users WHERE id = ?`, userID).
        Scan(&storedHash)
    if err == sql.ErrNoRows {
        // we don’t reveal “user doesn’t exist” vs “bad password”
        http.Error(w, "invalid user or password", http.StatusUnauthorized)
        return
    }
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        fmt.Printf("db select error: %v\n", err)
        return
    }

    // 2) verify the current password
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(currentPassword)); err != nil {
        // password did not match
        http.Error(w, "invalid user or password", http.StatusUnauthorized)
        return
    }

    // 3) generate a bcrypt hash of the new password
    //    cost can be bcrypt.DefaultCost, or a higher number if you need
    newHash, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        fmt.Printf("bcrypt generate error: %v\n", err)
        return
    }

    // 4) store the new hash
    _, err = db.Exec(`UPDATE users SET password_hash = ? WHERE id = ?`, newHash, userID)
    if err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        fmt.Printf("db update error: %v\n", err)
        return
    }

    // 5) success!
    w.WriteHeader(http.StatusNoContent)
}