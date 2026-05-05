package main

import (
    "fmt"
    "net/http"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    fmt.Println("Listening on :8080")
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    // simple hard-coded credentials (for demonstration only)
    if username == "moderator" && password == "hunter2" {
        fmt.Fprint(w, "true")
        return
    }
    fmt.Fprint(w, "false")
}