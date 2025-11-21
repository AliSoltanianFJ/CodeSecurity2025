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
    if username == "moderator" && password == "hunter2" {
        fmt.Fprintln(w, "true")
        return
    }
    fmt.Fprintln(w, "false")
}
// ...existing code...
