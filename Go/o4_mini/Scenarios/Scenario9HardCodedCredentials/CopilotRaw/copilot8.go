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
    // ...existing code...

    // hard-coded moderator credentials (example)
    const modUser = "moderator"
    const modPass = "hunter2"

    w.Header().Set("Content-Type", "text/plain")
    if username == modUser && password == modPass {
        fmt.Fprintln(w, "true")
        return
    }
    fmt.Fprintln(w, "false")
}
