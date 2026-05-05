package main

import (
    "encoding/json"
    "net/http"
    "fmt"
    "log"
    "os"
    "bufio"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    modHashMap      map[string][]byte
    dummyHash      []byte
    usernameRegexp = regexp.MustCompile(`^[A-Za-z0-9._-]{1,64}$`)
)

// init loads moderator bcrypt hashes from environment or file and prepares a dummy hash
func init() {
    modHashMap = make(map[string][]byte)

    // Prefer JSON map in env: MOD_USERS_JSON='{"alice":"$2a$...","bob":"$2a$..."}'
    if j := strings.TrimSpace(os.Getenv("MOD_USERS_JSON")); j != "" {
        var m map[string]string
        if err := json.Unmarshal([]byte(j), &m); err != nil {
            log.Fatalf("failed to parse MOD_USERS_JSON: %v", err)
        }
        for u, h := range m {
            modHashMap[u] = []byte(h)
        }
    }

    // Fallback: file with "username:bcrypthash" per line (MOD_USERS_FILE)
    if len(modHashMap) == 0 {
        if path := strings.TrimSpace(os.Getenv("MOD_USERS_FILE")); path != "" {
            f, err := os.Open(path)
            if err != nil {
                log.Fatalf("failed to open MOD_USERS_FILE: %v", err)
            }
            defer f.Close()
            sc := bufio.NewScanner(f)
            for sc.Scan() {
                line := strings.TrimSpace(sc.Text())
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
                    modHashMap[user] = []byte(hash)
                }
            }
            if err := sc.Err(); err != nil {
                log.Fatalf("error reading MOD_USERS_FILE: %v", err)
            }
        }
    }

    // Ensure we have at least one moderator configured in non-production/testing
    if len(modHashMap) == 0 {
        log.Println("warning: no moderator credentials loaded (set MOD_USERS_JSON or MOD_USERS_FILE)")
    }

    // Generate a dummy bcrypt hash to use for non-existent users to mitigate user enumeration/timing differences.
    // Use a fixed iteration cost (DefaultCost). This is not a secret.
    var err error
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("invalid-password"), bcrypt.DefaultCost)
    if err != nil {
        log.Fatalf("failed to generate dummy bcrypt hash: %v", err)
    }
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)

    // NOTE: In production, use TLS (ListenAndServeTLS or a reverse proxy terminating TLS).
    server := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }
    log.Println("listening on :8080")
    if err := server.ListenAndServe(); err != nil {
        log.Fatal(err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce safe method: do not accept credentials in URL/query string.
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Minimal response headers for APIs
    w.Header().Set("Content-Type", "application/json")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("X-Content-Type-Options", "nosniff")

    // Accept either JSON body {"username":"...","password":"..."} or form POST.
    var username, password string
    ct := r.Header.Get("Content-Type")
    if strings.Contains(ct, "application/json") {
        dec := json.NewDecoder(r.Body)
        var payload struct {
            Username string `json:"username"`
            Password string `json:"password"`
        }
        if err := dec.Decode(&payload); err != nil {
            // Generic error, avoid leaking parse details
            http.Error(w, `{"moderator":false}`, http.StatusForbidden)
            return
        }
        username = payload.Username
        password = payload.Password
    } else {
        // Parse form with size limits already governed by server ReadTimeout; still limit memory usage.
        if err := r.ParseForm(); err != nil {
            http.Error(w, `{"moderator":false}`, http.StatusForbidden)
            return
        }
        username = r.PostFormValue("username")
        password = r.PostFormValue("password")
    }

    // Input validation & canonicalization
    username = strings.TrimSpace(username)
    if !usernameRegexp.MatchString(username) {
        http.Error(w, `{"moderator":false}`, http.StatusForbidden)
        return
    }
    if len(password) == 0 || len(password) > 128 {
        http.Error(w, `{"moderator":false}`, http.StatusForbidden)
        return
    }

    // Lookup stored bcrypt hash; if not present, use dummyHash to mitigate username enumeration/timing.
    storedHash, ok := modHashMap[username]
    if !ok {
        storedHash = dummyHash
    }

    // Compare bcrypt hashes (constant-time behavior provided by bcrypt).
    err := bcrypt.CompareHashAndPassword(storedHash, []byte(password))
    isMod := (err == nil) && ok // Only true if bcrypt matched AND username actually exists.

    if isMod {
        w.WriteHeader(http.StatusOK)
        fmt.Fprint(w, `{"moderator":true}`)
        return
    }

    // Delay a small time to further reduce timing differences (keeps CPU low).
    time.Sleep(50 * time.Millisecond)
    http.Error(w, `{"moderator":false}`, http.StatusForbidden)
}
