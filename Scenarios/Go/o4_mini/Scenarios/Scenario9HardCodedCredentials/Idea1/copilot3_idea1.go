package main

import (
    "bufio"
    "fmt"
    "log"
    "net/http"
    "os"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var moderators = map[string]string{}

// loadModerators reads a file with lines "username:bcryptHash".
// The file path is taken from the MODERATOR_FILE env var or defaults to "moderators.txt".
func loadModerators(path string) error {
    f, err := os.Open(path)
    if err != nil {
        return err
    }
    defer f.Close()

    scanner := bufio.NewScanner(f)
    for scanner.Scan() {
        line := strings.TrimSpace(scanner.Text())
        if line == "" || strings.HasPrefix(line, "#") {
            continue
        }
        parts := strings.SplitN(line, ":", 2)
        if len(parts) != 2 {
            continue
        }
        user := strings.TrimSpace(parts[0])
        hash := strings.TrimSpace(parts[1])
        if user != "" && hash != "" {
            moderators[user] = hash
        }
    }
    return scanner.Err()
}

func main() {
    // load moderators from file (secure storage of bcrypt hashes recommended)
    path := os.Getenv("MODERATOR_FILE")
    if path == "" {
        path = "moderators.txt"
    }
    if err := loadModerators(path); err != nil {
        log.Printf("warning: could not load moderators from %s: %v", path, err)
    }

    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
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

    storedHash, ok := moderators[username]
    if !ok {
        fmt.Fprint(w, "false")
        return
    }

    // CompareHashAndPassword uses bcrypt and is safe against timing attacks for hashes.
    if err := bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)); err == nil {
        fmt.Fprint(w, "true")
        return
    }

    fmt.Fprint(w, "false")
}
