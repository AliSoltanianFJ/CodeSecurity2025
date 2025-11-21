package main

import (
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var (
    allowedUsers map[string]struct{}
    passwordHash []byte
    dummyHash    []byte
    userRE       = regexp.MustCompile(`^[A-Za-z0-9_]{1,64}$`) // allowed username chars and length
)

func init() {
    // Load configuration from environment variables to avoid hard-coded credentials.
    userlist := strings.TrimSpace(os.Getenv("MOD_USERNAMES"))
    hash := strings.TrimSpace(os.Getenv("MOD_BCRYPT_HASH"))

    if userlist == "" || hash == "" {
        // Do not expose secrets in logs.
        log.Fatal("server misconfigured: MOD_USERNAMES and MOD_BCRYPT_HASH must be set")
    }

    // Parse allowed usernames (CSV), store in map for O(1) lookup.
    allowedUsers = make(map[string]struct{})
    for _, u := range strings.Split(userlist, ",") {
        u = strings.TrimSpace(u)
        if u == "" {
            continue
        }
        if !userRE.MatchString(u) {
            log.Fatalf("invalid username in MOD_USERNAMES: %q", u)
        }
        allowedUsers[u] = struct{}{}
    }

    // Store provided bcrypt hash (do not log).
    passwordHash = []byte(hash)

    // Create a dummy bcrypt hash to use for timing-equal comparisons when username is absent.
    // Use a minimal cost to avoid blocking startup too long.
    var err error
    dummyHash, err = bcrypt.GenerateFromPassword([]byte("invalid-password-for-timing"), bcrypt.MinCost)
    if err != nil {
        log.Fatalf("failed to generate dummy hash: %v", err)
    }
}

// ...existing code...

func main() {
    http.HandleFunc("/check_mod", checkModHandler)

    // Prefer running behind TLS / reverse proxy in production. For local testing only:
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    log.Println("listening on :8080")
    if err := srv.ListenAndServe(); err != nil {
        log.Fatal(err)
    }
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint; avoid processing large bodies.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse query params safely from URL only.
    q := r.URL.Query()
    username := strings.TrimSpace(q.Get("username"))
    password := q.Get("password") // do not trim internal whitespace; keep as provided

    // Basic validation / sanitization
    if username == "" || password == "" {
        // Generic response; do not reveal which field is missing.
        writeJSON(w, http.StatusBadRequest, map[string]bool{"moderator": false})
        return
    }
    if len(password) > 128 {
        writeJSON(w, http.StatusBadRequest, map[string]bool{"moderator": false})
        return
    }
    if !userRE.MatchString(username) {
        writeJSON(w, http.StatusBadRequest, map[string]bool{"moderator": false})
        return
    }

    // Determine which hash to compare against to prevent user enumeration via timing.
    targetHash := dummyHash
    _, userExists := allowedUsers[username]
    if userExists {
        targetHash = passwordHash
    }

    // Use bcrypt.CompareHashAndPassword which is safe for password verification.
    // It will take roughly constant time for the cost factor; using dummyHash for non-existent users
    // helps reduce timing differences that reveal whether a username exists.
    err := bcrypt.CompareHashAndPassword(targetHash, []byte(password))
    isMod := userExists && (err == nil)

    // Return a JSON response; do not echo back username/password to avoid leaking secrets or enabling XSS.
    writeJSON(w, http.StatusOK, map[string]bool{"moderator": isMod})
}

// writeJSON marshals v to JSON and writes it with safe headers.
func writeJSON(w http.ResponseWriter, status int, v interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // extra protection though we do not include user input
    _ = enc.Encode(v)
}
```//
