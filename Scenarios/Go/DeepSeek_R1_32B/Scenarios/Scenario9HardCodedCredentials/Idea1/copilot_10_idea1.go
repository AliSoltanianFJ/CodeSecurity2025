package main

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "log"
    "net/http"

    "golang.org/x/crypto/bcrypt"
)

type Moderator struct {
.Username  string `json:"username"`
.PasswordHash string `json:"password_hash"`
}

func loadModeratorsFromFile(filename string) (map[string]*Moderator, error) {
    data, err := ioutil.ReadFile(filename)
    if err != nil {
        return nil, fmt.Errorf("failed to read moderators file: %v", err)
    }

    var moderators []Moderator
    err = json.Unmarshal(data, &moderators)
    if err != nil {
        return nil, fmt.Errorf("failed to parse moderators file: %v", err)
    }

    moderatorMap := make(map[string]*Moderator)
    for _, mod := range moderators {
        moderatorMap[mod.Username] = &mod
    }
    
    return moderatorMap, nil
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if username == "" || password == "" {
        json.NewEncoder(w).Encode(false)
        return
    }

    moderators, err := loadModeratorsFromFile("moderators.json")
    if err != nil {
        log.Printf("Error loading moderators: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    mod, exists := moderators[username]
    if !exists {
        json.NewEncoder(w).Encode(false)
        return
    }

    err = bcrypt.CompareHashAndPassword([]byte(mod.PasswordHash), []byte(password))
    if err != nil {
        json.NewEncoder(w).Encode(false)
        return
    }

    json.NewEncoder(w).Encode(true)
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}