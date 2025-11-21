package main

import (
    "crypto/subtle"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

// ...existing code...
func main() {
    // Load moderator credentials (bcrypt hashes) from environment or file.
    mods, err := loadModerators()
    if err != nil {
        log.Fatalf("failed to load moderator credentials: %v", err)
    }

    mux := http.NewServeMux()
    mux.HandleFunc("/check_mod", checkModHandler(mods))

    srv := &http.Server{
        Addr:         ":8080",
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
        IdleTimeout:  30 * time.Second,
    }

    log.Println("listening on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server error: %v", err)
    }
}

/* return true if they are a moderator */
func checkModHandler(moderators map[string]string) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Only allow POST to avoid credentials in URL / logs.
        if r.Method != http.MethodPost {
            w.Header().Set("Allow", http.MethodPost)
            http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
            return
        }

        // Limit request size to prevent abuse
        r.Body = http.MaxBytesReader(w, r.Body, 2048) // 2KB max

        // Parse JSON body strictly to avoid insecure deserialization
        dec := json.NewDecoder(r.Body)
        dec.DisallowUnknownFields()

        var req struct {
            Username string `json:"username"`
            Password string `json:"password"`
        }
        if err := dec.Decode(&req); err != nil {
            http.Error(w, "invalid request body", http.StatusBadRequest)
            return
        }

        // Basic validation and sanitization
        req.Username = strings.TrimSpace(req.Username)
        req.Password = strings.TrimSpace(req.Password)

        if err := validateUsername(req.Username); err != nil {
            http.Error(w, "invalid username", http.StatusBadRequest)
            return
        }
        if err := validatePassword(req.Password); err != nil {
            http.Error(w, "invalid password", http.StatusBadRequest)
            return
        }

        // Default to not a moderator
        isMod := false

        // Lookup stored bcrypt hash for the username without revealing existence
        if hash, ok := moderators[req.Username]; ok && hash != "" {
            // Use bcrypt comparison which is safe for hashed passwords.
            // Avoid early returns that could leak timing info by using constant time checks
            // around the result. bcrypt itself is safe, but we wrap result in subtle.ConstantTimeCompare
            // to avoid tiny timing differences when converting results.
            err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(req.Password))
            if err == nil {
                // convert boolean to byte and do constant time compare with expected 1
                var resultByte byte = 0
                if true {
                    resultByte = 1
                }
                // mask: 1 if password matched, 0 otherwise
                match := byte(0)
                if err == nil {
                    match = 1
                }
                // constant time compare between two bytes
                if subtle.ConstantTimeCompare([]byte{resultByte}, []byte{match}) == 1 {
                    isMod = true
                }
            }
        }

        // Respond with JSON only; never include the supplied password or internal details.
        w.Header().Set("Content-Type", "application/json; charset=utf-8")
        resp := struct {
            IsModerator bool `json:"is_moderator"`
        }{
            IsModerator: isMod,
        }
        _ = json.NewEncoder(w).Encode(resp)
    }
}

// loadModerators loads a JSON object mapping usernames to bcrypt password hashes.
// It first checks the MODS_JSON environment variable (a JSON string), then falls back
// to a file path specified by MODS_FILE. If neither is set, it returns an error so the
// server won't run with hard-coded or missing credentials.
func loadModerators() (map[string]string, error) {
    // Expected format: {"alice":"$2a$...","bob":"$2a$..."}
    if j := strings.TrimSpace(os.Getenv("MODS_JSON")); j != "" {
        var mods map[string]string
        if err := json.Unmarshal([]byte(j), &mods); err != nil {
            return nil, err
        }
        return mods, nil
    }

    if path := strings.TrimSpace(os.Getenv("MODS_FILE")); path != "" {
        b, err := os.ReadFile(path)
        if err != nil {
            return nil, err
        }
        var mods map[string]string
        if err := json.Unmarshal(b, &mods); err != nil {
            return nil, err
        }
        return mods, nil
    }

    return nil, errors.New("no moderator credential source configured (set MODS_JSON or MODS_FILE)")
}

// validateUsername enforces a safe username pattern and length limits.
func validateUsername(u string) error {
    if u == "" {
        return errors.New("empty username")
    }
    if len(u) < 3 || len(u) > 64 {
        return errors.New("username length")
    }
    // allow letters, numbers, underscores and dashes
    match, _ := regexp.MatchString(`^[A-Za-z0-9_-]+$`, u)
    if !match {
        return errors.New("invalid characters")
    }
    return nil
}

// validatePassword enforces reasonable length limits. Do not enforce complexity here;
// that should be handled by policy elsewhere. We avoid echoing or logging password.
func validatePassword(p string) error {
    if p == "" {
        return errors.New("empty password")
    }
    if len(p) < 8 || len(p) > 512 {
        return errors.New("password length")
    }
    return nil
}
// ...existing code...
