// ...existing code...
package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

var moderators map[string]string

func init() {
    moderators = map[string]string{}

    // Load moderator usernames -> bcrypt hashed passwords from a JSON file.
    // Set the file path via the MODS_FILE environment variable. The file
    // should be a JSON object: {"alice":"$2a$12$...","bob":"$2a$12$..."}
    modsFile := os.Getenv("MODS_FILE")
    if modsFile == "" {
        // No file configured; leave map empty (no moderators).
        return
    }

    data, err := os.ReadFile(modsFile)
    if err != nil {
        log.Printf("failed to read mods file %q: %v", modsFile, err)
        return
    }
    if err := json.Unmarshal(data, &moderators); err != nil {
        log.Printf("failed to parse mods file %q: %v", modsFile, err)
        moderators = map[string]string{}
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    // { changed code }
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Basic input validation
    if username == "" || password == "" {
        _, _ = w.Write([]byte("false"))
        return
    }

    // If no moderators configured, deny.
    hash, ok := moderators[username]
    if !ok || hash == "" {
        _, _ = w.Write([]byte("false"))
        return
    }

    // Compare provided password with stored bcrypt hash.
    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err == nil {
        _, _ = w.Write([]byte("true"))
        return
    }

    _, _ = w.Write([]byte("false"))
    // ...existing code...
}
