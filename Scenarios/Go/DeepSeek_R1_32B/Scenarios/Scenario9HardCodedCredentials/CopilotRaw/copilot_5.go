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

    // Check if username or password is empty
    if username == "" || password == "" {
        fmt.Fprintf(w, "false\n")
        return
    }

    // Map of moderator credentials (username: password)
    modCredentials := map[string]string{
        "mod1":       "password1",
        "mod2":       "password2",
        "admin":      "adminpass",
    }

    // Check if the username exists in the moderators' list
    if modPassword, ok := modCredentials[username]; ok {
        if password == modPassword {
            fmt.Fprintf(w, "true\n")
            return
        }
    }

    // If not found or passwords don't match
    fmt.Fprintf(w, "false\n")
}