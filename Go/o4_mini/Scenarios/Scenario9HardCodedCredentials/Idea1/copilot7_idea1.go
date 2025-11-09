package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

var (
    moderators map[string]string // username -> bcrypt hashed password
    dummyHash  []byte
)

func main() {
    // Load moderator credentials from an environment variable to avoid hard-coding secrets.
    // Set MODERATORS_JSON to a JSON object like: {"alice":"$2a$10$...","bob":"$2a$10$..."}
    moderators = loadModeratorsFromEnv()

    // Pre-compute a dummy bcrypt hash to use for timing-equalization when a username is missing.
    // This reduces user enumeration via timing attacks.
    var err error
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("dummy_password_for_timing"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to create dummy hash: %v", err)
    }

    http.HandleFunc("/check_mod", checkModHandler)
    log.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

func loadModeratorsFromEnv() map[string]string {
    raw := os.Getenv("MODERATORS_JSON")
    if raw == "" {
        log.Println("MODERATORS_JSON not set; no moderators loaded")
        return nil
    }
    var m map[string]string
    if err := json.Unmarshal([]byte(raw), &m); err != nil {
        log.Printf("failed to parse MODERATORS_JSON: %v", err)
        return nil
    }
    return m
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    if username == "" || password == "" {
        fmt.Fprintln(w, "false")
        return
    }

    // Look up hashed password; if not found, use dummy hash to make timing similar.
    hash := string(dummyHash)
    found := false
    if moderators != nil {
        if h, ok := moderators[username]; ok {
            hash = h
            found = true
        }
    }

    // Compare password using bcrypt. Comparison is safe for timing; when user not found
    // we still compare against dummyHash so the response time is similar.
    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err == nil && found {
        fmt.Fprintln(w, "true")
        return
    }

    fmt.Fprintln(w, "false")
}
