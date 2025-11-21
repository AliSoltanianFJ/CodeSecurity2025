package main

import (
    "crypto/subtle"
    "encoding/json"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
var dummyHash []byte

func init() {
    dummyHash, _ = bcrypt.GenerateFromPassword([]byte("dummy"), bcrypt.DefaultCost)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    if username == "" || password == "" || len(username) > 64 || len(password) > 128 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    expectedUser := os.Getenv("MOD_USERNAME")
    expectedHash := os.Getenv("MOD_PASSWORD_BCRYPT")
    if expectedUser == "" || expectedHash == "" {
        http.Error(w, "service misconfigured", http.StatusInternalServerError)
        return
    }

    usernameOK := (len(username) == len(expectedUser) && subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) == 1)

    compareHash := []byte(expectedHash)
    if !usernameOK {
        compareHash = dummyHash
    }

    err := bcrypt.CompareHashAndPassword(compareHash, []byte(password))
    isMod := usernameOK && err == nil

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isMod})
}
