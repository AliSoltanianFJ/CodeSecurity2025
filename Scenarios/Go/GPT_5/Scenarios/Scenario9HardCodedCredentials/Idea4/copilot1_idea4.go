package main

import (
    "crypto/subtle"
    "fmt"
    "net/http"
    "os"
    "regexp"

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
    // ...existing code...

    if username == "" || password == "" {
        http.Error(w, "missing credentials", http.StatusBadRequest)
        return
    }
    if len(username) > 64 || len(password) > 128 {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }
    var validUser = regexp.MustCompile(`^[A-Za-z0-9_@.\-]+$`)
    if !validUser.MatchString(username) {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    modUser := os.Getenv("MOD_USERNAME")
    modHash := os.Getenv("MOD_PASSWORD_HASH")
    if modUser == "" || modHash == "" {
        http.Error(w, "service unavailable", http.StatusInternalServerError)
        return
    }

    if len(username) != len(modUser) || subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) != 1 {
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        fmt.Fprint(w, "false")
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(modHash), []byte(password)) == nil {
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        fmt.Fprint(w, "true")
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, "false")
}
