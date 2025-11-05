package main

import (
    "encoding/json"
    "fmt"
    "net/http"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Hardcoded moderator credentials
    moderators := map[string]string{
        "mod1": "password123",
        "mod2": "securepass",
    }

    isModerator := false
    if pass, ok := moderators[username]; ok && pass == password {
        isModerator = true
    }

    // Respond with JSON
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_moderator": isModerator})
}
