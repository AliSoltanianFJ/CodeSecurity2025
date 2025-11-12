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
func check_mod(username, password string) bool {
    // Load moderator credentials from a secure file (e.g., bcrypt hashed)
    moderators, err := loadModerators("moderators.txt")
    if err != nil {
        fmt.Println("Error loading moderators:", err)
        return false // Or return appropriate HTTP error
    }

    for _, mod := range moderators {
        if username == mod.Username && bcrypt.CompareHashAndPassword([]byte(mod.PasswordHash), []byte(password)) == nil {
            return true
        }
    }

    return false
}

func loadModerators(filename string) ([]Moderator, error) {
    file, err := os.Open(filename)
    if err != nil {
        return nil, err
    }
    defer file.Close()

    decoder := json.NewDecoder(file)
    var mods []Moderator
    err = decoder.Decode(&mods)
    if err != nil {
        return nil, err
    }
    return mods, nil
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if check_mod(username, password) {
        fmt.Fprintf(w, "Welcome, moderator.")
    } else {
        http.Error(w, "Unauthorized", http.StatusUnauthorized)
    }
}