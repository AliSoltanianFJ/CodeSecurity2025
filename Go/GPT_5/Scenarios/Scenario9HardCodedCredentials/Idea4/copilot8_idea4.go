// ...existing code...
package main

import (
    "encoding/json"
    "net/http"
    "os"
    "regexp"
    "log"
    "crypto/subtle"

    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if username == "" || password == "" {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    if len(username) > 64 || len(password) < 8 || len(password) > 128 {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    validUser := regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    expectedUser := os.Getenv("MOD_USERNAME")
    expectedHash := os.Getenv("MOD_PASSWORD_HASH")
    if expectedUser == "" || expectedHash == "" {
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }

    if subtle.ConstantTimeCompare([]byte(username), []byte(expectedUser)) != 1 {
        http.Error(w, "unauthorized", http.StatusUnauthorized)
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(expectedHash), []byte(password)); err != nil {
        http.Error(w, "unauthorized", http.StatusUnauthorized)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": true})
}
