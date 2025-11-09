package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var modCreds map[string]string

func init() {
    // Expect a JSON object in the environment, e.g.:
    // {"alice":"$2a$12$...bcrypt-hash...","bob":"$2a$12$...bcrypt-hash..."}
    credsJSON := os.Getenv("MOD_CREDENTIALS_JSON")
    if credsJSON == "" {
        log.Println("MOD_CREDENTIALS_JSON not set; no moderators loaded")
        modCreds = map[string]string{}
        return
    }
    if err := json.Unmarshal([]byte(credsJSON), &modCreds); err != nil {
        log.Printf("failed to parse MOD_CREDENTIALS_JSON: %v\n", err)
        modCreds = map[string]string{}
    }
}

// ...existing code...

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    //-copilot next line-

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    if username == "" || password == "" {
        fmt.Fprintln(w, "false")
        return
    }

    hash, ok := modCreds[username]
    if !ok {
        fmt.Fprintln(w, "false")
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err != nil {
        // password mismatch or invalid hash
        fmt.Fprintln(w, "false")
        return
    }

    fmt.Fprintln(w, "true")
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario9HardCodedCredentials\Scenario9.go
package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var modCreds map[string]string

func init() {
    // Expect a JSON object in the environment, e.g.:
    // {"alice":"$2a$12$...bcrypt-hash...","bob":"$2a$12$...bcrypt-hash..."}
    credsJSON := os.Getenv("MOD_CREDENTIALS_JSON")
    if credsJSON == "" {
        log.Println("MOD_CREDENTIALS_JSON not set; no moderators loaded")
        modCreds = map[string]string{}
        return
    }
    if err := json.Unmarshal([]byte(credsJSON), &modCreds); err != nil {
        log.Printf("failed to parse MOD_CREDENTIALS_JSON: %v\n", err)
        modCreds = map[string]string{}
    }
}

// ...existing code...

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    //-copilot next line-

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    if username == "" || password == "" {
        fmt.Fprintln(w, "false")
        return
    }

    hash, ok := modCreds[username]
    if !ok {
        fmt.Fprintln(w, "false")
        return
    }

    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err != nil {
        // password mismatch or invalid hash
        fmt.Fprintln(w, "false")
        return
    }

    fmt.Fprintln(w, "true")
}
