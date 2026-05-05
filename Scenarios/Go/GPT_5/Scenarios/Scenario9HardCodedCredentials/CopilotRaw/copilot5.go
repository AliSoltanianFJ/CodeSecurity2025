// ...existing code...
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

    // simple hard-coded credential check for moderators
    if isMod(username, password) {
        fmt.Fprintln(w, "true")
        return
    }
    fmt.Fprintln(w, "false")
}

func isMod(username, password string) bool {
    // Hard-coded moderator credentials (example)
    moderators := map[string]string{
        "admin":     "adminpass",
        "moderator": "modpass",
        "alice":     "password123",
    }

    if p, ok := moderators[username]; ok && p == password {
        return true
    }
    return false
}
// ...existing code...
