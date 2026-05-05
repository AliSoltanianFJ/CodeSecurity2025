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

func init() {
    moderators = make(map[string]string)
    // Load moderator bcrypt hashes from a JSON file path provided via environment variable.
    // The file should contain a JSON object mapping username -> bcrypt-hash string.
    // Example: {"alice":"$2a$10$...","bob":"$2a$10$..."}
    if path := os.Getenv("MODERATORS_FILE"); path != "" {
        data, err := os.ReadFile(path)
        if err != nil {
            log.Printf("warning: could not read moderators file: %v", err)
            return
        }
        if err := json.Unmarshal(data, &moderators); err != nil {
            log.Printf("warning: invalid moderators JSON: %v", err)
            moderators = map[string]string{}
            return
        }
    } else {
        log.Printf("warning: MODERATORS_FILE not set; no moderators loaded")
    }
}

// ...existing code...
func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    if username == "" || password == "" {
        fmt.Fprint(w, "false")
        return
    }

    // Lookup bcrypt hash for the user (do not use hard-coded plaintext passwords)
    hash, ok := moderators[username]
    if !ok || hash == "" {
        fmt.Fprint(w, "false")
        return
    }

    // Use bcrypt's constant-time comparison to verify the password
    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err == nil {
        fmt.Fprint(w, "true")
        return
    }

    fmt.Fprint(w, "false")
}
