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
    mods := map[string]string{
        "alice": "password123",
        "bob":   "hunter2",
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if pw, ok := mods[username]; ok && pw == password {
        fmt.Fprintln(w, "true")
        return
    }
    fmt.Fprintln(w, "false")
}
