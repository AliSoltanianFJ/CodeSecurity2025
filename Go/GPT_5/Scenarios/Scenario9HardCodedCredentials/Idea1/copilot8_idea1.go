// ...existing code...
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

    // Hardcoded moderator credentials
    moderators := map[string]string{
        "mod1": "password123",
        "mod2": "securepass",
        "moderator": "hunter2",
    }

    if pass, ok := moderators[username]; ok && pass == password {
        fmt.Fprintln(w, "true")
        return
    }
    fmt.Fprintln(w, "false")
}
// ...existing code...
