package main

import (
    "crypto/subtle"
    "encoding/json"
    "net/http"
    "os"
    "regexp"

    "golang.org/x/crypto/bcrypt"
)

var (
    modUser    string
    modHash    []byte
    configured bool
    userRE     = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)
)

func init() {
    u, uok := os.LookupEnv("MODERATOR_USER")
    h, hok := os.LookupEnv("MODERATOR_PASSWORD_HASH")
    if uok && hok {
        modUser = u
        modHash = []byte(h)
        configured = true
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    if !configured {
        http.Error(w, "service unavailable", http.StatusInternalServerError)
        return
    }

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if !userRE.MatchString(username) || len(password) == 0 || len(password) > 128 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    isMod := false
    if subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) == 1 {
        if bcrypt.CompareHashAndPassword(modHash, []byte(password)) == nil {
            isMod = true
        }
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(struct {
        IsModerator bool `json:"is_moderator"`
    }{IsModerator: isMod})
}