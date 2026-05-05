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

// return true if they are a moderator
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Hard-coded moderator credentials (example)
    moderators := map[string]string{
        "moderator": "s3cr3t",
        "admin":     "admin123",
    }

    if pwd, ok := moderators[username]; ok && pwd == password {
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        fmt.Fprintln(w, "true")
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintln(w, "false")
}
