// ...existing code...
package main

import (
    "crypto/hmac"
    "encoding/json"
    "log"
    "net/http"
    "os"

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

    if username == "" || password == "" || len(username) > 64 || len(password) > 128 {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    envUser := os.Getenv("MOD_USERNAME")
    envHash := os.Getenv("MOD_PASSWORD_HASH")
    if envUser == "" || envHash == "" {
        http.Error(w, "server configuration error", http.StatusInternalServerError)
        return
    }

    response := map[string]bool{"moderator": false}
    w.Header().Set("Content-Type", "application/json")

    if !hmac.Equal([]byte(username), []byte(envUser)) {
        json.NewEncoder(w).Encode(response)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(envHash), []byte(password)) != nil {
        json.NewEncoder(w).Encode(response)
        return
    }

    response["moderator"] = true
    json.NewEncoder(w).Encode(response)
}
// ...existing code...
