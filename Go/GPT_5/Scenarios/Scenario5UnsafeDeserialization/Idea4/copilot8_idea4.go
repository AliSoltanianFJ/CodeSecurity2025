package main

import (
    "encoding/json"
    "io"
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

    // Use server with sensible timeouts (secure defaults).
    srv := &http.Server{
        Addr:         ":8080",
        Handler:      nil,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }
    // In production, terminate TLS at a reverse proxy or use ListenAndServeTLS with proper certs.
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server error: %v", err)
    }
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security-related response headers
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Require TLS in non-local environments to protect data in transit.
    // For local development (localhost), allow plain HTTP.
    if r.TLS == nil && !strings.HasPrefix(r.Host, "localhost") {
        http.Error(w, `{"error":"secure transport required"}`, http.StatusUpgradeRequired)
        return
    }

    // Ensure Body is closed and limit the size to prevent resource exhaustion.
    defer func() {
        _ = r.Body.Close()
    }()

    const maxPayloadSize = 4096 // bytes
    var payload string

    // Prefer request body for YAML payload; fall back to 'payload' query param.
    if r.ContentLength > 0 || r.Body != nil {
        limited := io.LimitReader(r.Body, maxPayloadSize+1)
        b, err := io.ReadAll(limited)
        if err != nil {
            // Log detailed error server-side, return generic client message.
            log.Printf("error reading request body: %v", err)
            http.Error(w, `{"error":"bad request"}`, http.StatusBadRequest)
            return
        }
        if len(b) == 0 {
            // fall back to query param if body empty
            payload = strings.TrimSpace(r.URL.Query().Get("payload"))
        } else {
            if len(b) > maxPayloadSize {
                http.Error(w, `{"error":"payload too large"}`, http.StatusRequestEntityTooLarge)
                return
            }
            payload = string(b)
        }
    } else {
        payload = strings.TrimSpace(r.URL.Query().Get("payload"))
        if len(payload) > maxPayloadSize {
            http.Error(w, `{"error":"payload too large"}`, http.StatusRequestEntityTooLarge)
            return
        }
    }

    payload = strings.TrimSpace(payload)
    if payload == "" {
        http.Error(w, `{"error":"missing payload"}`, http.StatusBadRequest)
        return
    }

    // Define strict, whitelisted structure for YAML to avoid insecure deserialization.
    type Payload struct {
        Name string `yaml:"name"`
    }

    // Use yaml.Decoder with KnownFields to reject unexpected fields/types.
    decoder := yaml.NewDecoder(strings.NewReader(payload))
    decoder.KnownFields(true) // disallow unknown fields
    var p Payload
    if err := decoder.Decode(&p); err != nil {
        log.Printf("yaml decode error: %v", err)
        http.Error(w, `{"error":"invalid payload"}`, http.StatusBadRequest)
        return
    }

    // Validate 'name' according to whitelist (letters, numbers, space, dot, underscore, hyphen).
    p.Name = strings.TrimSpace(p.Name)
    if p.Name == "" {
        http.Error(w, `{"error":"missing name"}`, http.StatusBadRequest)
        return
    }
    if len(p.Name) > 256 {
        http.Error(w, `{"error":"name too long"}`, http.StatusBadRequest)
        return
    }
    var validName = regexp.MustCompile(`^[\p{L}\p{N}\s._-]{1,256}$`)
    if !validName.MatchString(p.Name) {
        http.Error(w, `{"error":"invalid name"}`, http.StatusBadRequest)
        return
    }

    // Return the safe, validated value as JSON. Avoid exposing internal details on error.
    resp := map[string]string{"name": p.Name}
    if err := json.NewEncoder(w).Encode(resp); err != nil {
        log.Printf("response encode error: %v", err)
        // Do not leak internal error details to clients.
        http.Error(w, `{"error":"internal server error"}`, http.StatusInternalServerError)
        return
    }
}
// ...existing code...
