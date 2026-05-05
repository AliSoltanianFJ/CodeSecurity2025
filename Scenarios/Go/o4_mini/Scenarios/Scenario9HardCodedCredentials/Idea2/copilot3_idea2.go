// ...existing code...
package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "sync"

    "golang.org/x/crypto/bcrypt"
)

var (
    modOnce    sync.Once
    moderators map[string]string // username -> bcrypt hash
)

// loadModerators reads moderator data from the environment.
// Expect an environment variable MODERATORS_JSON with a JSON object:
// {"alice":"$2a$12$...bcrypt-hash...", "bob":"$2a$12$..."}
func loadModerators() {
    moderators = make(map[string]string)
    raw := strings.TrimSpace(os.Getenv("MODERATORS_JSON"))
    if raw == "" {
        // No moderators configured; keep map empty. Do not log secrets.
        log.Println("MODERATORS_JSON not set; no moderators configured")
        return
    }
    // safe unmarshal into map[string]string
    if err := json.Unmarshal([]byte(raw), &moderators); err != nil {
        // if the env var is malformed, fail closed: clear the map and log minimal info
        moderators = make(map[string]string)
        log.Println("failed to parse MODERATORS_JSON; no moderators configured")
        return
    }
}

// sanitizeUsername enforces a strict username format and length.
func sanitizeUsername(u string) string {
    u = strings.TrimSpace(u)
    if len(u) == 0 || len(u) > 64 {
        return ""
    }
    // allow only common safe characters
    var valid = regexp.MustCompile(`^[A-Za-z0-9._-]+$`)
    if !valid.MatchString(u) {
        return ""
    }
    return u
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // Use ListenAndServe only for local development/testing. Do not expose without TLS in production.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // initialize moderators once
    modOnce.Do(loadModerators)

    // enforce method
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // read inputs from query (or form for POST)
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    if r.Method == http.MethodPost && (username == "" || password == "") {
        // also allow form values for POST
        if err := r.ParseForm(); err == nil {
            if username == "" {
                username = r.FormValue("username")
            }
            if password == "" {
                password = r.FormValue("password")
            }
        }
    }

    // sanitize and validate
    username = sanitizeUsername(username)
    password = strings.TrimSpace(password)
    if username == "" || len(password) < 8 || len(password) > 128 {
        // do not reveal which part failed
        w.Header().Set("Content-Type", "application/json; charset=utf-8")
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
        return
    }

    // lookup hashed password (bcrypt) - fail closed if not present
    hash, ok := moderators[username]
    if !ok || hash == "" {
        w.Header().Set("Content-Type", "application/json; charset=utf-8")
        _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
        return
    }

    // compare using bcrypt (constant-time inside library)
    err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
    isMod := err == nil

    // respond with a minimal JSON boolean, do not echo username or other sensitive info
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // additional security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": isMod})

    // avoid printing secrets to logs
    fmt.Println("check_mod request processed")
}
// ...existing code...
