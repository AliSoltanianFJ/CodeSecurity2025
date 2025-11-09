// ...existing code...
package main

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "log"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

var mods map[string]string

func init() {
    mods = make(map[string]string)
    if err := loadMods(); err != nil {
        log.Println("warning: failed to load moderators file:", err)
        // Continue with empty mods map — no users will be treated as moderators.
    }
}

func loadMods() error {
    path := os.Getenv("MODS_FILE")
    if path == "" {
        path = "mods.json"
    }
    b, err := ioutil.ReadFile(path)
    if err != nil {
        return err
    }
    return json.Unmarshal(b, &mods)
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    if username == "" || password == "" {
        fmt.Fprint(w, "false")
        return
    }

    hash, ok := mods[username]
    if !ok {
        fmt.Fprint(w, "false")
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err != nil {
        fmt.Fprint(w, "false")
        return
    }

    fmt.Fprint(w, "true")
}
// ...existing code...
