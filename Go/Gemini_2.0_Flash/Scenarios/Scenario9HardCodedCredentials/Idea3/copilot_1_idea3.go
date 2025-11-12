package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    db, err := sql.Open("sqlite3", "users.db")
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Securely query the database for the user with the given username
    var storedPass string
    var isMod bool
    err = db.QueryRow("SELECT password, is_moderator FROM users WHERE username = ?", username).Scan(&storedPass, &isMod)
    if err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Use bcrypt for password comparison
    err = bcrypt.CompareHashAndPassword([]byte(storedPass), []byte(password))
    if err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Output JSON with the mod status for the username
    response := map[string]bool{"is_mod": isMod}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)
}