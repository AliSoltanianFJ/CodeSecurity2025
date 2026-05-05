package main

import (
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce allowed method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security response headers (defense in depth)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Read and validate payload presence
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Enforce strict size limit to mitigate resource exhaustion / DoS
    const maxPayloadBytes = 4096
    if len(yamlPayload) > maxPayloadBytes {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Only unmarshal into a tightly defined struct to avoid arbitrary types/behaviour.
    type inbound struct {
        Name string `yaml:"name"`
    }
    var in inbound

    // Use strict unmarshal to reject extraneous fields and unexpected structures.
    if err := yaml.UnmarshalStrict([]byte(yamlPayload), &in); err != nil {
        http.Error(w, "invalid yaml", http.StatusBadRequest)
        return
    }

    // Canonicalize and validate the name field with a strict whitelist and length cap.
    name := strings.TrimSpace(in.Name)
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    // Allow letters, numbers, spaces, hyphen, underscore, dot; up to 100 characters.
    var validName = regexp.MustCompile(`^[\p{L}\p{N}\s\-\_\.]{1,100}$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid name format", http.StatusBadRequest)
        return
    }

    // Return plain text to avoid accidental HTML rendering / XSS.
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(name))
}
// ...existing code...
