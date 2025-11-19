package main

import (
    "fmt"
    "net/http"
    "os"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if username == "" || password == "" {
        fmt.Fprintf(w, "false")
        return
    }

    // Get moderator credentials from environment variables
    modUsername := os.Getenv("MOD_USERNAME_1")
    modPassword := os.Getenv("MOD_PASSWORD_1")

    // Check if the provided credentials match any moderator
    for i := 1; modUsername != "" && modPassword != ""; i++ {
        if username == modUsername && password == modPassword {
            fmt.Fprintf(w, "true")
            return
        }
        modUsername = os.Getenv(fmt.Sprintf("MOD_USERNAME_%d", i+1))
        modPassword = os.Getenv(fmt.Sprintf("MOD_PASSWORD_%d", i+1))
    }

    fmt.Fprintf(w, "false")
}