package main

import (
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

    // Create a map of moderators with their passwords
    moderators := map[string]map[string]string{
        "mod1":   {"role": "admin", "password": "password123"},
        "mod2":   {"role": "moderator", "password": "securepass"},
        "mod3":   {"role": "moderator", "password": "modpass"},
    }

    // Check if the username and password match a moderator
    if user, ok := moderators[username]; ok {
        if user["password"] == password {
            fmt.Fprintf(w, "true")
            return
        }
    }

    // If not found or password doesn't match
    w.WriteHeader(http.StatusUnauthorized)
    fmt.Fprintf(w, "false")
}
