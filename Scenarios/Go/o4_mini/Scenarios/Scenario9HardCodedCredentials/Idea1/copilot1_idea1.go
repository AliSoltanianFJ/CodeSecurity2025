package main

import (
    "encoding/json"
    "fmt"
    "net/http"
    "os"
    "sync"

    "golang.org/x/crypto/bcrypt"
)

var (
    mods     map[string]string // username -> bcrypt password hash
    loadErr  error
    loadOnce sync.Once
)

// loadMods loads a JSON file with a map of username->bcrypt-hash.
// Path can be overridden with the MODS_FILE environment variable.
// Example mods.json: {"alice":"$2a$10$...bcrypt-hash...", "bob":"$2a$10$..."}
func loadMods() {
    path := os.Getenv("MODS_FILE")
    if path == "" {
        path = "mods.json"
    }
    data, err := os.ReadFile(path)
    if err != nil {
        loadErr = err
        return
    }
    var m map[string]string
    if err := json.Unmarshal(data, &m); err != nil {
        loadErr = err
        return
    }
    mods = m
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // basic input validation
    if username == "" || password == "" {
        fmt.Fprint(w, "false")
        return
    }

    // load moderator list once (from external JSON file)
    loadOnce.Do(loadMods)
    if loadErr != nil {
        // don't leak details to clients
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // lookup bcrypt hash for the username and compare securely
    if hash, ok := mods[username]; ok {
        if bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) == nil {
            fmt.Fprint(w, "true")
            return
        }
    }

    fmt.Fprint(w, "false")
}