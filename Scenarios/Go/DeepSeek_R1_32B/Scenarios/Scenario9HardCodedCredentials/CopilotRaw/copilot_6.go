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

    // List of moderators and their passwords (for example purposes only)
    moderators := map[string]string{
        "mod1": "password123",
        "mod2": "securepass456",
    }

    if username == "" || password == "" {
        fmt.Fprint(w, "false")
        return
    }

    modPassword, exists := moderators[username]
    if !exists {
        fmt.Fprint(w, "false")
        return
    }

    if modPassword == password {
        fmt.Fprint(w, "true")
    } else {
        fmt.Fprint(w, "false")
    }
}