package main

import (
    "crypto/subtle"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// return true if they are a moderator
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password")

    if username == "" || password == "" || len(username) > 64 || len(password) > 128 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    modUser := os.Getenv("MOD_USERNAME")
    modPass := os.Getenv("MOD_PASSWORD")
    if modUser == "" || modPass == "" {
        http.Error(w, "service unavailable", http.StatusInternalServerError)
        return
    }

    userMatch := subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) == 1
    passMatch := subtle.ConstantTimeCompare([]byte(password), []byte(modPass)) == 1
    isMod := userMatch && passMatch

    w.Header().Set("Content-Type", "application/json")
    _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": isMod})
}
// ...existing code...
