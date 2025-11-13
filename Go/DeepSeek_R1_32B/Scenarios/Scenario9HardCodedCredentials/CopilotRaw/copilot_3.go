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

    type Moderator struct {
        username  string
        password  string
    }

    moderators := []Moderator{
        {"admin", "12345"},
        // Add more moderators as needed
    }

    var isMod bool
    for _, mod := range moderators {
        if username == mod.username && password == mod.password {
            isMod = true
            break
        }
    }

    fmt.Fprintf(w, "%v", isMod)
}