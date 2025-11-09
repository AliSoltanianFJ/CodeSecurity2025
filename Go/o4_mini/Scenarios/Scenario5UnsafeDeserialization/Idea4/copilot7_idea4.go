package main

import (
    "errors"
    "net/http"
    "regexp"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    // In production: use http.Server with timeouts and TLS configuration.
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security controls:
    // - Only allow GET for this endpoint (as designed). If POST/PUT needed, switch and validate body.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Get payload from query param (untrusted)
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Canonicalize: trim whitespace
    yamlPayload = strings.TrimSpace(yamlPayload)

    // Enforce strict maximum length to mitigate DoS via huge payloads.
    const maxPayloadBytes = 16 * 1024 // 16 KB
    if len(yamlPayload) == 0 || len(yamlPayload) > maxPayloadBytes {
        http.Error(w, "payload size invalid", http.StatusRequestEntityTooLarge)
        return
    }

    // Use a strict struct to avoid unsafe/ambiguous deserialization.
    type payload struct {
        Name string `yaml:"name"`
    }

    var p payload

    // Use the vetted yaml.v3 package. Unmarshal into the strict struct only.
    // Limit parsing time by using a timeout context pattern (simple timer here).
    parseDone := make(chan error, 1)
    go func() {
        parseDone <- yaml.Unmarshal([]byte(yamlPayload), &p)
    }()

    select {
    case err := <-parseDone:
        if err != nil {
            // Do not leak internal parse errors to the client.
            http.Error(w, "invalid payload format", http.StatusBadRequest)
            return
        }
    case <-time.After(2 * time.Second):
        // Parsing took too long — possible DoS or pathological input.
        http.Error(w, "payload parse timeout", http.StatusRequestTimeout)
        return
    }

    // Validate the extracted "name" field with a strict whitelist.
    // Acceptable: letters, digits, spaces, hyphen, underscore, max length 256.
    if err := validateName(p.Name); err != nil {
        http.Error(w, "invalid name", http.StatusBadRequest)
        return
    }

    // Response hardening headers
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Return only the validated name (no extra metadata).
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(p.Name))
}

// validateName applies whitelist rules to the name field.
func validateName(name string) error {
    name = strings.TrimSpace(name)
    if name == "" {
        return errors.New("empty")
    }
    if len(name) > 256 {
        return errors.New("too long")
    }
    // Allow alphanumerics, spaces, hyphen, underscore, period.
    var validName = regexp.MustCompile(`^[A-Za-z0-9 _\-.]+$`)
    if !validName.MatchString(name) {
        return errors.New("invalid characters")
    }
    return nil
}
