package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"
)

type Moderator struct {
.Username string `json:"username"`
.Password string `json:"password"`
}

var modMap = make(map[string]string)

func main() {
    configFile, err := os.ReadFile("config.json")
    if err != nil {
        log.Fatal("Error reading config file:", err)
    }
    var moderators []Moderator
    err = json.Unmarshal(configFile, &moderators)
    if err != nil {
        log.Fatal("Error parsing config file:", err)
    }
    for _, mod := range moderators {
        modMap[mod.Username] = mod.Password
    }
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    if storedPassword, ok := modMap[username]; ok {
        if password == storedPassword {
            fmt.Fprintf(w, "true")
            return
        }
    }

    fmt.Fprintf(w, "false")
}