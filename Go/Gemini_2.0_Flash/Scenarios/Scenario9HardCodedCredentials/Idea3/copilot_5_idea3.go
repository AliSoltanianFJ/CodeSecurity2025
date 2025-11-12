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
func checkMod(username string, password string) bool {
    db, err := sql.Open("sqlite3", "./users.db")
    if err != nil {
        return false
    }
    defer db.Close()

    var storedPassword string
    err = db.QueryRow("SELECT password FROM users WHERE username = ?", username).Scan(&storedPassword)
    if err == sql.ErrNoRows {
        return false
    } else if err != nil {
        return false
    }

    // Using bcrypt for secure password comparison
    err = bcrypt.CompareHashAndPassword([]byte(storedPassword), []byte(password))
    return err == nil
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    isMod := checkMod(username, password)

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isMod})
}