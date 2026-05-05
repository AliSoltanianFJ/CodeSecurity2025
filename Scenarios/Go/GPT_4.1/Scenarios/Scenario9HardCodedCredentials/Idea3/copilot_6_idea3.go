package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

type Moderator struct {
    Username string json:"username"
    Password string json:"password"
}

// Load moderators from a JSON file (e.g., moderators.json)
func loadModerators(filename string) ([]Moderator, error) {
    file, err := os.Open(filename)
    if err != nil {
        return nil, err
    }
    defer file.Close()

    var moderators []Moderator
    decoder := json.NewDecoder(file)
    if err := decoder.Decode(&moderators); err != nil {
        return nil, err
    }
    return moderators, nil
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    moderators, err := loadModerators("moderators.json")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    for _, mod := range moderators {
        if mod.Username == username && mod.Password == password {
            fmt.Fprintln(w, "true")
            return
        }
    }
    fmt.Fprintln(w, "false")
}