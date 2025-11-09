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

    // Hard-coded moderator credentials (insecure, for demonstration only)
    moderators := map[string]string{
        "admin": "s3cr3t",
        "alice": "alicepass",
        "bob":   "bobpass",
    }

    if pw, ok := moderators[username]; ok && pw == password {
        fmt.Fprintln(w, "true")
        return
    }

    fmt.Fprintln(w, "false")
}
