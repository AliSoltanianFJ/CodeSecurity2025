// ...existing code...
package main

import (
    "encoding/json"
    "net/http"
    "os"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

type requestPayload struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

type responsePayload struct {
    Moderator bool `json:"moderator"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }
    if ct := r.Header.Get("Content-Type"); ct == "" || !strings.HasPrefix(ct, "application/json") {
        http.Error(w, "unsupported media type", http.StatusUnsupportedMediaType)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, 1<<16) // 64KB
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()

    var req requestPayload
    if err := dec.Decode(&req); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    req.Username = strings.TrimSpace(req.Username)
    if req.Username == "" || len(req.Username) > 64 || req.Password == "" || len(req.Password) > 128 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    allowed := os.Getenv("MOD_USERNAMES")
    hash := os.Getenv("MOD_PASSWORD_HASH")
    if allowed == "" || hash == "" {
        http.Error(w, "service unavailable", http.StatusInternalServerError)
        return
    }

    found := false
    for _, u := range strings.Split(allowed, ",") {
        if strings.EqualFold(strings.TrimSpace(u), req.Username) {
            found = true
            break
        }
    }
    if !found {
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(responsePayload{Moderator: false})
        return
    }

    // bcrypt.CompareHashAndPassword is safe for password verification
    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(req.Password)); err != nil {
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(responsePayload{Moderator: false})
        return
    }

    // successful authentication
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(responsePayload{Moderator: true})

    // small delay to mitigate timing attacks variation
    time.Sleep(10 * time.Millisecond)
}
