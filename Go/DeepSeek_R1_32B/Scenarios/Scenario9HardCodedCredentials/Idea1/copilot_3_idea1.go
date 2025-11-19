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

    // Load moderator credentials from environment variable
    modCredentialsEnv := os.Getenv("MOD_CREDENTIALS")
    if modCredentialsEnv == "" {
        json.NewEncoder(w).Encode(false)
        return
    }

    var modCredentials map[string]string
    err := json.Unmarshal([]byte(modCredentialsEnv), &modCredentials)
    if err != nil {
        fmt.Printf("Error parsing MOD_CREDENTIALS: %v\n", err)
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    // Check if username exists in credentials
    modPassword, exists := modCredentials[username]
    if !exists {
        json.NewEncoder(w).Encode(false)
        return
    }

    // Compare passwords
    if password == modPassword {
        json.NewEncoder(w).Encode(true)
    } else {
        json.NewEncoder(w).Encode(false)
    }
}