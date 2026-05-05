package main

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "log"
    "net/http"
    "os"
)

type Moderator struct {
.Username string `json:"username"`
.Password string `json:"password"`
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    var moderators []Moderator

    // Check for environment variables first
    modUsername := os.Getenv("MODUSERNAME")
    modPassword := os.Getenv("MODPASSWORD")

    if modUsername != "" && modPassword != "" {
        moderators = append(moderators, Moderator{
        .Username: modUsername,
            .Password: modPassword,
        })
    } else {
        // Read from configuration file
        data, err := ioutil.ReadFile("mod_config.json")
        if err != nil {
            log.Printf("Error reading config file: %v", err)
            w.WriteHeader(http.StatusInternalServerError)
            return
        }

        err = json.Unmarshal(data, &moderators)
        if err != nil {
            log.Printf("Error parsing config file: %v", err)
            w.WriteHeader(http.StatusBadRequest)
            return
        }
    }

    // Basic input validation
    if username == "" || password == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "false")
        return
    }

    // Check if the user is a moderator
    isMod := false
    for _, mod := range moderators {
        if username == mod.Username && password == mod.Password {
            isMod = true
            break
        }
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]bool{"is_mod": isMod})
}