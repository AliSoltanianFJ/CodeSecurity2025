package main

import (
    "encoding/json"
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

    result := checkMod(username, password)

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_mod": result})
}

func checkMod(username, password string) bool {
    // hardcoded moderator credentials for example purposes
    modCredentials := map[string]string{
        "admin":   "admin123",
        "moderator": "modpass456",
    }

    if _, ok := modCredentials[username]; !ok {
        return false
    }
    if modCredentials[username] != password {
        return false
    }
    return true
}