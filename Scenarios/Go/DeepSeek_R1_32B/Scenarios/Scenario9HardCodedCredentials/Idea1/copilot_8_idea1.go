package main

import (
    "encoding/json"
    "fmt"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

var modCredentials map[string]string

func init() {
    credsJSON := os.Getenv("MOD_CREDENTIALS")
    if credsJSON == "" {
        modCredentials = make(map[string]string)
        return
    }

    if err := json.Unmarshal([]byte(credsJSON), &modCredentials); err != nil {
        panic(fmt.Sprintf("failed to parse MOD_CREDENTIALS: %v", err))
    }
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if username == "" || password == "" {
        fmt.Fprint(w, "false")
        return
    }

    for u, h := range modCredentials {
        if u != username {
            continue
        }
        
        if err := bcrypt.CompareHashAndPassword([]byte(h), []byte(password)); err == nil {
            fmt.Fprint(w, "true")
            return
        }
    }

    fmt.Fprint(w, "false")
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}