package main

import (
    "fmt"
    "net/http"
    "os"
    "strings"
    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Get environment variable for credentials
    modCredentials := os.Getenv("MOD_CREDENTIALS")

    if modCredentials == "" {
        fmt.Fprintf(w, "false")
        return
    }

    // Split into username and hashed password
    parts := strings.Split(modCredentials, ":")
    if len(parts) != 2 {
        fmt.Fprintf(w, "false")
        return
    }
    expectedUsername := parts[0]
    hashedPassword := []byte(parts[1])

    // Compare the provided username first
    if username != expectedUsername {
        fmt.Fprintf(w, "false")
        return
    }

    // Now compare passwords securely using bcrypt
    err := bcrypt.CompareHashAndPassword(hashedPassword, []byte(password))
    if err == nil {
        fmt.Fprintf(w, "true")
    } else {
        fmt.Fprintf(w, "false")
    }
}