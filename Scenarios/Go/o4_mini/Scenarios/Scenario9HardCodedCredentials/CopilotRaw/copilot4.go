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

    // hard-coded moderator credentials (example)
    moderators := map[string]string{
        "admin": "password123",
        "alice": "s3cr3t",
        "bob":   "hunter2",
    }

    isMod := false
    if pwd, ok := moderators[username]; ok && pwd == password {
        isMod = true
    }

    fmt.Fprint(w, isMod)
}

// ...existing code...
