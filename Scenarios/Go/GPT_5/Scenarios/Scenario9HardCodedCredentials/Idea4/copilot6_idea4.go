// ...existing code...
package main

import (
    "crypto/subtle"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    q := r.URL.Query()
    username := q.Get("username")
    password := q.Get("password")

    if username == "" || password == "" {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    if len(username) < 3 || len(username) > 32 || len(password) > 128 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    var validUser = regexp.MustCompile(`^[A-Za-z0-9_-]{3,32}$`)
    if !validUser.MatchString(username) {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    modsEnv := os.Getenv("MODERATORS")
    if modsEnv == "" {
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }

    found := false
    for _, m := range strings.Split(modsEnv, ",") {
        if strings.EqualFold(strings.TrimSpace(m), username) {
            found = true
            break
        }
    }
    if !found {
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(false)
        return
    }

    envPwd := os.Getenv("MOD_PASSWORD")
    if envPwd == "" {
        http.Error(w, "service unavailable", http.StatusServiceUnavailable)
        return
    }

    allowed := 0
    if len(password) == len(envPwd) && subtle.ConstantTimeCompare([]byte(password), []byte(envPwd)) == 1 {
        allowed = 1
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(allowed == 1)
}
// ...existing code...
