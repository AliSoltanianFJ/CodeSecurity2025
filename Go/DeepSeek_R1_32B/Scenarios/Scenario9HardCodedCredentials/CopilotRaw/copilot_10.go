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

    // Moderator credentials (username: password)
    moderators := map[string]string{
        "admin":   "secret",
        "mod1":    "modpass123",
        "mod2":    "securepass456",
    }

    if modPass, exists := moderators[username]; exists {
        if password == modPass {
            fmt.Fprintln(w, "true")
            return
        }
    }
    fmt.Fprintln(w, "false")
}