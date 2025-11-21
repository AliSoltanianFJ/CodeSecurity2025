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

    // Hard-coded moderator credentials (example)
    moderators := map[string]string{
        "alice": "alice123",
        "bob":   "bobpass",
    }

    isMod := moderators[username] == password

    // simple logging
    fmt.Printf("check_mod: username=%s isMod=%v\n", username, isMod)

    if isMod {
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("true"))
        return
    }

    w.WriteHeader(http.StatusUnauthorized)
    w.Write([]byte("false"))
}
