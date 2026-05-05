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

// ...existing code...
/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // simple hard-coded moderator credentials
    moderators := map[string]string{
        "alice": "s3cr3t",
        "bob":   "hunter2",
    }

    isMod := false
    if pw, ok := moderators[username]; ok && pw == password {
        isMod = true
    }

    // respond with plain "true" or "false"
    fmt.Fprintln(w, isMod)
}

