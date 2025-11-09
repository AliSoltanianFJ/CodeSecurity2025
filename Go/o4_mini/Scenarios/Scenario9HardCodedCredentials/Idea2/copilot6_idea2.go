// ...existing code...
package main

import (
    "encoding/json"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
)

var (
    modCreds map[string][]byte
    userRe   = regexp.MustCompile(`^[A-Za-z0-9_.-]{1,64}$`)
    fakeHash []byte
)

// load credentials from environment variable MOD_CREDENTIALS_JSON
// format: {"alice":"$2a$10$....", "bob":"$2b$10$...."}
// This avoids hardcoding secrets in source. If the variable is missing or invalid,
// no moderator logins will be accepted.
func init() {
    modCreds = map[string][]byte{}
    raw := strings.TrimSpace(os.Getenv("MOD_CREDENTIALS_JSON"))
    if raw != "" {
        var temp map[string]string
        if err := json.Unmarshal([]byte(raw), &temp); err == nil {
            // validate entries and limit number to prevent resource issues
            hashRe := regexp.MustCompile(`^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$`)
            count := 0
            for u, h := range temp {
                if count >= 100 {
                    break
                }
                if !userRe.MatchString(u) || !hashRe.MatchString(h) {
                    continue
                }
                modCreds[u] = []byte(h)
                count++
            }
        } else {
            log.Println("MOD_CREDENTIALS_JSON parse error:", err)
        }
    } else {
        log.Println("MOD_CREDENTIALS_JSON not set; no moderators configured")
    }

    // Create a fake bcrypt hash to consume time when user not found (mitigates timing attacks).
    // Cost chosen to be reasonable; use a constant seed so hash is stable across runs.
    // This does not expose any secret.
    h, err := bcrypt.GenerateFromPassword([]byte("constant-fake-password-please-replace"), bcrypt.DefaultCost)
    if err != nil {
        // fallback to a minimal valid bcrypt hash pattern if generation fails (very unlikely)
        fakeHash = []byte("$2a$10$.....................................................")
    } else {
        fakeHash = h
    }
    // small sleep to avoid tight startup bursts in constrained environments
    time.Sleep(5 * time.Millisecond)
}

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    // NOTE: production must use TLS (ListenAndServeTLS) and proper reverse proxy/firewall.
    log.Println("starting server on :8080")
    http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET or POST
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form for POST bodies safely (small memory usage)
    if r.Method == http.MethodPost {
        if err := r.ParseForm(); err != nil {
            // do not leak internal errors
            http.Error(w, "bad request", http.StatusBadRequest)
            return
        }
    }

    // Retrieve values from either querystring or form safely
    username := strings.TrimSpace(firstNonEmpty(
        r.FormValue("username"),
        r.URL.Query().Get("username"),
    ))
    password := r.FormValue("password")
    if password == "" {
        password = r.URL.Query().Get("password")
    }
    password = strings.TrimSpace(password)

    // Basic length validation to avoid resource abuse
    if len(username) == 0 || len(username) > 64 || len(password) == 0 || len(password) > 128 {
        writeJSON(w, false)
        return
    }

    // Validate allowed username characters
    if !userRe.MatchString(username) {
        writeJSON(w, false)
        return
    }

    // Lookup stored hash; if not found, compare against fakeHash to normalize timing
    hash, ok := modCreds[username]
    if !ok {
        // perform compare with fake hash to equalize response timing
        _ = bcrypt.CompareHashAndPassword(fakeHash, []byte(password))
        writeJSON(w, false)
        return
    }

    // Compare password using bcrypt (secure, constant-time)
    if err := bcrypt.CompareHashAndPassword(hash, []byte(password)); err != nil {
        writeJSON(w, false)
        return
    }

    // Success
    writeJSON(w, true)
}

// writeJSON sends a minimal JSON response and ensures no user input is reflected unescaped.
func writeJSON(w http.ResponseWriter, ok bool) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Keep response body minimal to avoid leaking info
    resp := struct {
        Moderator bool `json:"moderator"`
    }{Moderator: ok}

    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // ensure output is safe for HTML contexts
    // ignore encode error -- if it fails, return a generic server error
    if err := enc.Encode(resp); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
    }
}

// firstNonEmpty returns the first non-empty string from the arguments.
func firstNonEmpty(vals ...string) string {
    for _, v := range vals {
        if s := strings.TrimSpace(v); s != "" {
            // escape any potential HTML if this value is ever used in output (we don't)
            return html.EscapeString(s)
        }
    }
    return ""
}
