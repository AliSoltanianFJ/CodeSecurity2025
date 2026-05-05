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

    // Hard-coded moderator credentials (insecure — for demo only)
    if username == "moderator" && password == "modpass" {
        fmt.Fprint(w, "true")
        return
    }

    fmt.Fprint(w, "false")
}
