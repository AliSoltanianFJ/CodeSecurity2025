package main

import (
    "fmt"
    "net/http"
)

// ...existing code...

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if isModerator(username, password) {
        fmt.Fprintln(w, "true")
        return
    }
    fmt.Fprintln(w, "false")
}

// helper: hard-coded moderators (demo only)
var moderators = map[string]string{
    "admin": "s3cr3t",
    "mod1":  "password1",
}

func isModerator(username, password string) bool {
    if pwd, ok := moderators[username]; ok && pwd == password {
        return true
    }
    return false
}

// ...existing code...
