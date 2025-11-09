package main

import (
    "crypto/subtle"
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

    // hard-coded moderator credentials (username -> password)
    mods := map[string]string{
        "alice": "s3cr3t",
        "bob":   "hunter2",
    }

    if expected, ok := mods[username]; ok &&
        subtle.ConstantTimeCompare([]byte(expected), []byte(password)) == 1 {
        fmt.Fprintln(w, "true")
        return
    }

    fmt.Fprintln(w, "false")
}

// ...existing code...
