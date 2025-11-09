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

    "crypto/subtle"

    "golang.org/x/crypto/bcrypt"
)

var (
    // restrict username to a safe subset and reasonable length
    usernamePattern = regexp.MustCompile(`^[A-Za-z0-9_\-]{1,64}$`)
    maxPasswordLen  = 128
)

// ...existing code...
func main() {
    // Read moderator credentials from environment (never hard-code secrets)
    modUser := strings.TrimSpace(os.Getenv("MOD_USERNAME"))
    modHash := os.Getenv("MOD_PASSWORD_HASH")

    // Fail fast on misconfiguration to avoid running with insecure defaults
    if modUser == "" || modHash == "" {
        log.Fatal("server configuration error: MOD_USERNAME and MOD_PASSWORD_HASH must be set")
    }
    if !usernamePattern.MatchString(modUser) {
        log.Fatal("server configuration error: MOD_USERNAME contains invalid characters or is too long")
    }
    if _, err := bcrypt.Cost([]byte(modHash)); err != nil {
        log.Fatal("server configuration error: MOD_PASSWORD_HASH is not a valid bcrypt hash")
    }

    // capture validated credentials in closure to avoid reading env per-request
    http.HandleFunc("/check_mod", func(w http.ResponseWriter, r *http.Request) {
        checkModHandler(w, r, modUser, modHash)
    })

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }

    log.Println("starting server on :8080")
    // In production, run behind TLS (ListenAndServeTLS) or a reverse proxy that terminates TLS.
    log.Fatal(srv.ListenAndServe())
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request, modUser, modHash string) {
    // Only allow simple safe methods
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse form data (supports query and POST form)
    if err := r.ParseForm(); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    // Read and normalise input
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Basic validation to avoid abusive or malformed input
    if username == "" || !usernamePattern.MatchString(username) {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }
    if len(password) == 0 || len(password) > maxPasswordLen {
        http.Error(w, "invalid credentials", http.StatusBadRequest)
        return
    }

    // Constant-time username compare to reduce timing-based user enumeration
    usernameMatch := subtle.ConstantTimeCompare([]byte(username), []byte(modUser)) == 1

    // Always perform bcrypt comparison (with the configured hash) to keep timing similar
    // bcrypt is safe for password verification. We do not log or reveal specifics.
    pwErr := bcrypt.CompareHashAndPassword([]byte(modHash), []byte(password))

    isMod := usernameMatch && pwErr == nil

    // Prepare JSON response. Do not echo sensitive input back.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Use a small response structure; ensure any text would be escaped by encoder.
    resp := struct {
        Moderator bool   `json:"moderator"`
        Message   string `json:"message,omitempty"`
    }{
        Moderator: isMod,
    }

    // Provide a generic message only when not moderator to avoid leaking details.
    if !isMod {
        resp.Message = "authentication failed"
        // sanitize message (though this is constant)
        resp.Message = html.EscapeString(resp.Message)
    }

    // Encode response
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}
// ...existing code...
