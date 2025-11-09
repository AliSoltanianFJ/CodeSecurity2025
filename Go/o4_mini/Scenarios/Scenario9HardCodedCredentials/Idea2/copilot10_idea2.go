// ...existing code...
package main

import (
    "crypto/subtle"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    modUser    string
    modHash    []byte
    userRegexp = regexp.MustCompile(`^[A-Za-z0-9_-]{1,64}$`)
)

func main() {
    // Load moderator credentials from environment - never hardcode secrets in source.
    modUser = os.Getenv("MOD_USER")
    hash := os.Getenv("MOD_PASSWORD_HASH") // bcrypt hash expected

    if modUser == "" || hash == "" {
        log.Fatal("server misconfigured: set MOD_USER and MOD_PASSWORD_HASH environment variables")
    }

    // Keep stored hash in memory only, do not log it.
    var err error
    modHash = []byte(hash)
    // Validate provided hash format by attempting to compare a dummy password to avoid later panic on malformed hash.
    if _, err = bcrypt.Cost(modHash); err != nil {
        log.Fatal("server misconfigured: MOD_PASSWORD_HASH is not a valid bcrypt hash")
    }

    http.HandleFunc("/check_mod", checkModHandler)

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
        Handler:      nil,
    }
    log.Println("starting server on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatal(err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint (uses query parameters).
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and validate inputs
    q := r.URL.Query()
    username := q.Get("username")
    password := q.Get("password")

    // Basic validation: present and reasonable lengths
    if username == "" || password == "" {
        respondJSON(w, false)
        return
    }
    if len(username) > 64 || len(password) > 128 {
        respondJSON(w, false)
        return
    }
    // Restrict username characters to a safe subset to avoid injection/XSS risks
    if !userRegexp.MatchString(username) {
        respondJSON(w, false)
        return
    }

    // To avoid user enumeration and timing attacks, perform constant-time equality for username,
    // and always perform a password hash compare even when username mismatches (using a dummy hash).
    usernameMatch := subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) == 1

    // Use a dummy bcrypt hash if username doesn't match to keep timing similar.
    dummyHash := modHash
    if !usernameMatch {
        // A fixed dummy bcrypt hash for timing protection (generated from a random password).
        // This avoids exposing whether username exists by timing.
        dummyHash = []byte("$2a$10$abcdefghijklmnopqrstuv") // intentionally invalid cost will not be used because Cost check done at startup for real hash
        // Attempt a safe fallback: use modHash as dummy as well to keep behavior consistent.
        dummyHash = modHash
    }

    // Compare password using bcrypt. Do not reveal reason for failure.
    err := bcrypt.CompareHashAndPassword(dummyHash, []byte(password))
    if err != nil || !usernameMatch {
        respondJSON(w, false)
        return
    }

    // Success
    respondJSON(w, true)
}

func respondJSON(w http.ResponseWriter, ok bool) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Do not reflect any user-controlled data back to avoid XSS.
    resp := struct {
        Moderator bool `json:"moderator"`
    }{Moderator: ok}

    enc := json.NewEncoder(w)
    // Encoder in Go escapes HTML characters by default which helps prevent XSS if any strings are returned.
    _ = enc.Encode(resp)
}
