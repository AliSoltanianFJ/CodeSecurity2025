package main

import (
    "fmt"
    "log"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
var moderators map[string]string

func init() {
    // Load moderator username:password-hash pairs from the environment variable MODERATORS.
    // Expected format: "alice:$2b$...;bob:$2b$..."
    // Do NOT hard-code credentials in source control.
    moderators = parseModeratorsFromEnv(os.Getenv("MODERATORS"))
}

func parseModeratorsFromEnv(s string) map[string]string {
    m := make(map[string]string)
    if s == "" {
        return m
    }
    pairs := strings.Split(s, ";")
    for _, p := range pairs {
        p = strings.TrimSpace(p)
        if p == "" {
            continue
        }
        parts := strings.SplitN(p, ":", 2)
        if len(parts) != 2 {
            continue
        }
        user := strings.TrimSpace(parts[0])
        hash := strings.TrimSpace(parts[1])
        if user != "" && hash != "" {
            m[user] = hash
        }
    }
    return m
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    // ...existing code...

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Basic validation
    if username == "" || password == "" {
        fmt.Fprintln(w, "false")
        return
    }

    // Lookup stored bcrypt hash for the username (loaded from env)
    hash, ok := moderators[username]
    if !ok || hash == "" {
        fmt.Fprintln(w, "false")
        return
    }

    // Compare password with stored bcrypt hash. bcrypt handles timing-resistant comparison.
    if err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)); err == nil {
        fmt.Fprintln(w, "true")
        return
    }

    fmt.Fprintln(w, "false")
}
