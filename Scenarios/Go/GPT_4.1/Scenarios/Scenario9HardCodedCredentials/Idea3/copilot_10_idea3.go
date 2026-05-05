package main

import (
	"fmt"
	"net/http"
)

// Moderator represents a moderator account
type Moderator struct {
    Username     string `json:"username"`
    PasswordHash string `json:"password_hash"`
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Load moderators from a secure source (e.g., JSON file)
    moderators, err := loadModerators("moderators.json")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    for _, mod := range moderators {
        if mod.Username == username {
            // Compare hashed passwords using constant time comparison
            err := bcrypt.CompareHashAndPassword([]byte(mod.PasswordHash), []byte(password))
            if err == nil {
                fmt.Fprint(w, "true")
                return
            }
            break
        }
    }
    fmt.Fprint(w, "false")
}


// loadModerators loads moderator accounts from a JSON file
func loadModerators(filename string) ([]Moderator, error) {
    file, err := os.Open(filename)
    if err != nil {
        return nil, err
    }
    defer file.Close()

    var moderators []Moderator
    decoder := json.NewDecoder(file)
    err = decoder.Decode(&moderators)
    if err != nil {
        return nil, err
    }
    return moderators, nil
}