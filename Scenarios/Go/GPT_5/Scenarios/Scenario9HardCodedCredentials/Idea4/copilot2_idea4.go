package main

import (
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var (
    moderators           map[string]string
    moderatorsConfigured bool
    usernameRe           = regexp.MustCompile(`^[A-Za-z0-9_-]{3,32}$`)
)

func init() {
    data := os.Getenv("MODERATORS_JSON")
    if data == "" {
        moderatorsConfigured = false
        return
    }
    var m map[string]string
    if err := json.Unmarshal([]byte(data), &m); err != nil {
        log.Printf("invalid MODERATORS_JSON")
        moderatorsConfigured = false
        return
    }
    moderators = m
    moderatorsConfigured = true
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if !moderatorsConfigured {
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }
    var req struct {
        Username string `json:"username"`
        Password string `json:"password"`
    }
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&req); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    if !usernameRe.MatchString(req.Username) || len(req.Password) < 8 || len(req.Password) > 128 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    hash, ok := moderators[req.Username]
    isMod := false
    if ok {
        if bcrypt.CompareHashAndPassword([]byte(hash), []byte(req.Password)) == nil {
            isMod = true
        }
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isMod})
}

// ...existing code...
