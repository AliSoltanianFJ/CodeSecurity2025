package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
var moderators map[string]string
var fakeHash string

func init() {
    moderators = make(map[string]string)

    // Expect MOD_CREDENTIALS to be a JSON object mapping usernames to bcrypt hashes:
    // e.g. {"alice":"$2a$10$...","bob":"$2a$10$..."}
    data := os.Getenv("MOD_CREDENTIALS")
    if data == "" {
        log.Println("MOD_CREDENTIALS not set; no moderators loaded")
    } else {
        if err := json.Unmarshal([]byte(data), &moderators); err != nil {
            log.Println("failed to parse MOD_CREDENTIALS:", err)
            moderators = make(map[string]string)
        }
    }

    // Generate a fake hash to normalize timing for non-existent users.
    h, _ := bcrypt.GenerateFromPassword([]byte("invalidpassword"), bcrypt.DefaultCost)
    fakeHash = string(h)
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    // ...existing code...

    hash, ok := moderators[username]
    if !ok {
        // Use the fake hash so the bcrypt comparison takes similar time
        // whether or not the username exists (helps prevent user enumeration).
        hash = fakeHash
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err == nil && ok {
        _, _ = w.Write([]byte("true"))
        return
    }

    _, _ = w.Write([]byte("false"))
}
