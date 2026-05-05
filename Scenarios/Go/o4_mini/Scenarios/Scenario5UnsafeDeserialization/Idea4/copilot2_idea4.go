package main

import (
    "html"
    "io"
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this simple API (reduce attack surface)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read payload from query param
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Protect against large payloads (DoS)
    const maxPayloadSize = 4096 // bytes
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Restrict to printable ASCII + common whitespace to avoid binary/poison chars
    printable := regexp.MustCompile(`^[\x09\x0A\x0D\x20-\x7E]+$`)
    if !printable.MatchString(yamlPayload) {
        http.Error(w, "invalid payload characters", http.StatusBadRequest)
        return
    }

    // Decode YAML into a strict, minimal struct to avoid deserializing arbitrary types.
    type Payload struct {
        Name string `yaml:"name"`
    }
    var p Payload

    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    // Require known fields only — reject payloads with unexpected fields
    dec.KnownFields(true)
    if err := dec.Decode(&p); err != nil {
        http.Error(w, "invalid yaml", http.StatusBadRequest)
        return
    }

    // Validate the name field: non-empty, reasonable length, whitelist characters
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Allow letters, numbers, spaces, dot, underscore, dash (unicode letters & numbers)
    validName := regexp.MustCompile(`^[\p{L}\p{N} ._\-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Response hardening
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Escape output to prevent XSS if consumed in a browser context
    _, _ = io.WriteString(w, html.EscapeString(name))
}
