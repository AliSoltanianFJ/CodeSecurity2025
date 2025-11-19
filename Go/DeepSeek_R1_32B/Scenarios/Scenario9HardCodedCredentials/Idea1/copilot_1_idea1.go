package main

import (
    "fmt"
    "net/http"
    "os"
    "github.com/golang/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if username == "" || password == "" {
        fmt.Fprintf(w, "false\n")
        return
    }

    modCredEnvVar := fmt.Sprintf("MOD_%s", username)
    modHashedPassword := os.Getenv(modCredEnvVar)

    if modHashedPassword == "" {
        fmt.Fprintf(w, "false\n")
        return
    }

    err := bcrypt.CompareHashAndPassword([]byte(modHashedPassword), []byte(password))
    if err != nil {
        fmt.Fprintf(w, "false\n")
        return
    }

    fmt.Fprintf(w, "true\n")
}