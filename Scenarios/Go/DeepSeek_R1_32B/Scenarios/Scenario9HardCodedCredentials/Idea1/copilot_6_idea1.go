package main

import (
    "encoding/json"
    "fmt"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

var modCredentials = make(map[string][]byte)

func init() {
    modUser1 := os.Getenv("MOD_USER_1")
    modPass1 := os.Getenv("MOD_PASS_1_HASHED")
    if modUser1 != "" && modPass1 != "" {
        hashed, _ := bcrypt.DecodeBase64(modPass1)
        modCredentials[modUser1] = hashed
    }

    modUser2 := os.Getenv("MOD_USER_2")
    modPass2 := os.Getenv("MOD_PASS_2_HASHED")
    if modUser2 != "" && modPass2 != "" {
        hashed, _ := bcrypt.DecodeBase64(modPass2)
        modCredentials[modUser2] = hashed
    }
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if username == "" || password == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Username and password are required.")
        return
    }

    hashedPassword, exists := modCredentials[username]
    if !exists {
        w.WriteHeader(http.StatusUnauthorized)
        fmt.Fprint(w, false)
        return
    }

    err := bcrypt.CheckPasswordHash(password, hashedPassword)
    if err != nil {
        w.WriteHeader(http.StatusUnauthorized)
        fmt.Fprint(w, false)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_mod": true})
}

[GIN] 2025/11/19 - 03:53:17 | 200 |         1m33s |       127.0.0.1 | POST     "/api/chat"

⏱️ Time taken: 93.96 seconds