package main

import (
    "encoding/json"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)

    // Use a small, explicit server configuration (no Read/Write timeouts would be insecure).
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    log.Println("listening on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (defense-in-depth)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    // Minimal Content-Security-Policy to prevent inline execution if content ever becomes HTML
    w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")

    // Only accept GET for this simple handler (explicit allow-list)
    if r.Method != http.MethodGet {
        http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
        return
    }

    rawPayload := r.URL.Query().Get("payload")
    if rawPayload == "" {
        http.Error(w, `{"error":"missing payload"}`, http.StatusBadRequest)
        return
    }

    // Enforce reasonable size limits to mitigate resource exhaustion
    const maxPayloadLen = 4096
    if len(rawPayload) > maxPayloadLen {
        http.Error(w, `{"error":"payload too large"}`, http.StatusRequestEntityTooLarge)
        return
    }

    // Normalize whitespace
    rawPayload = strings.TrimSpace(rawPayload)

    // Define a strict target type for YAML unmarshal (avoid interface{} or arbitrary types)
    type requestYAML struct {
        Name string `yaml:"name"`
    }

    var req requestYAML
    if err := yaml.Unmarshal([]byte(rawPayload), &req); err != nil {
        // Do not include raw payload or PII in logs
        log.Printf("yaml unmarshal error: %v", err)
        http.Error(w, `{"error":"invalid yaml payload"}`, http.StatusBadRequest)
        return
    }

    // Validate the extracted name using a whitelist regex.
    // Allow Unicode letters, numbers, spaces, and a small set of punctuation, length 1..255.
    var nameRe = regexp.MustCompile(`^[\p{L}\p{N}][\p{L}\p{N} \-_'\.]{0,254}$`)
    req.Name = strings.TrimSpace(req.Name)
    if req.Name == "" || !nameRe.MatchString(req.Name) {
        http.Error(w, `{"error":"invalid name field"}`, http.StatusBadRequest)
        return
    }

    // Escape the value for safety in any context; JSON encoder will also escape as needed.
    safeName := html.EscapeString(req.Name)

    // Return the name in a small JSON object (avoid echoing full payload)
    resp := map[string]string{"name": safeName}
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        // Minimal error returned to client, log without PII
        log.Printf("json encode error: %v", err)
        http.Error(w, `{"error":"internal server error"}`, http.StatusInternalServerError)
        return
    }
}
