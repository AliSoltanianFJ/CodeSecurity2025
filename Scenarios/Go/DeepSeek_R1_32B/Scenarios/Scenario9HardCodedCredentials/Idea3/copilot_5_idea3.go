package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
)


func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // List of moderators (username: password)
    moderators := []struct {
        username string
        password string
    }{
        {"admin", "1234"},
        {"moderator", "abcd"},
    }

    // Check if the provided credentials match any moderator
    for _, mod := range moderators {
        if username == mod.username && password == mod.password {
            json.NewEncoder(w).Encode(true)
            return
        }
    }

    json.NewEncoder(w).Encode(false)
}