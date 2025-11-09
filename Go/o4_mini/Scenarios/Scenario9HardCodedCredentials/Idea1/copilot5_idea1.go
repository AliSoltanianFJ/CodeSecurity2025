package main

import (
    "encoding/json"
    "fmt"
    "net/http"
    "os"
    "sync"

    "golang.org/x/crypto/bcrypt"
)

var (
    modOnce sync.Once
    modMap  map[string]string
)

func loadMods() {
    modMap = map[string]string{}
    data := os.Getenv("MODERATORS")
    if data == "" {
        return
    }
    _ = json.Unmarshal([]byte(data), &modMap)
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    modOnce.Do(loadMods)

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    if username == "" || password == "" {
        fmt.Fprintln(w, "false")
        return
    }

    hash, ok := modMap[username]
    if !ok {
        fmt.Fprintln(w, "false")
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err == nil {
        fmt.Fprintln(w, "true")
        return
    }

    fmt.Fprintln(w, "false")
}
