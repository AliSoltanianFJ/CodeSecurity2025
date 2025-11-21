// ...existing code...
package main

import (
    "encoding/json"
    "net/http"
    "os"
    "regexp"

    "golang.org/x/crypto/bcrypt"
    "crypto/subtle"
)

var (
    modUser    string
    modPassHash []byte
    dummyHash  []byte
    usernameRE = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)
)

func init() {
    modUser = os.Getenv("MOD_USERNAME")
    if h := os.Getenv("MOD_PASSWORD_HASH"); h != "" {
        modPassHash = []byte(h)
    }
    dummyHash, _ = bcrypt.GenerateFromPassword([]byte("invalid-password-for-timing"), bcrypt.DefaultCost)
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

    if username == "" || password == "" || len(password) > 128 || !usernameRE.MatchString(username) {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    compareHash := dummyHash
    userMatches := subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) == 1
    if userMatches && len(modPassHash) > 0 {
        compareHash = modPassHash
    }

    _ = bcrypt.CompareHashAndPassword(compareHash, []byte(password))
    isMod := userMatches && bcrypt.CompareHashAndPassword(compareHash, []byte(password)) == nil

    w.Header().Set("Content-Type", "application/json")
    _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": isMod})
}
