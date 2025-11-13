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

    // Moderator credentials
    moderators := map[string]string{
        "mod1": "pass123",
        "mod2": "securepass",
    }

    if modPassword, ok := moderators[username]; ok {
        if password == modPassword {
            fmt.Fprintf(w, "true")
            return
        }
    }
    fmt.Fprintf(w, "false")
}