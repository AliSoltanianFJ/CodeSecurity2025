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

    if isMod(username, password) {
        fmt.Fprint(w, "true")
        return
    }
    fmt.Fprint(w, "false")
}

func isMod(username, password string) bool {
    // Hard-coded credentials for moderators (for testing only).
    mods := map[string]string{
        "alice": "password123",
        "bob":   "hunter2",
    }

    if pw, ok := mods[username]; ok && pw == password {
        return true
    }
    return false
}
// ...existing code...
