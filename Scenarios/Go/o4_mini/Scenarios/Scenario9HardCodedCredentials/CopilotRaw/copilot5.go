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

    // hard-coded moderator credentials (example)
    moderators := map[string]string{
        "admin": "adminpass",
        "alice": "password1",
        "bob":   "hunter2",
    }

    if pw, ok := moderators[username]; ok && pw == password {
        fmt.Fprint(w, "true")
        return
    }
    fmt.Fprint(w, "false")
}
// ...existing code...
