package main

import (
    "encoding/json"
    "html"
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
    // Security controls:
    // - Limit input size (basic check on param length).
    // - Decode YAML strictly into a whitelisted struct (no dynamic types).
    // - Validate and canonicalize "name" using a strict whitelist regexp.
    // - Escape output and return JSON.
    // - Set secure headers.
    const maxPayloadLen = 4096
    const maxNameLen = 256

    // Only allow GET and POST for this demo; reject others.
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Retrieve payload from query param (as per existing API). Trim whitespace.
    yamlPayload := strings.TrimSpace(r.URL.Query().Get("payload"))
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    if len(yamlPayload) > maxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Decode strictly into a known struct to avoid dynamic type instantiation.
    type requestPayload struct {
        Name string `yaml:"name"`
    }
    var p requestPayload

    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    dec.KnownFields(true) // reject unknown fields
    if err := dec.Decode(&p); err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate the name field: present, not too long, and matches a strict whitelist.
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name required", http.StatusBadRequest)
        return
    }
    if len(name) > maxNameLen {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Allow letters, numbers, space, hyphen, underscore, and dot (unicode letters supported).
    var nameRe = regexp.MustCompile(`^[\p{L}\p{N} _\.\-]+$`)
    if !nameRe.MatchString(name) {
        http.Error(w, "name contains invalid characters", http.StatusBadRequest)
        return
    }

    // Output: escape to prevent reflected XSS and encode as JSON.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Basic security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    out := map[string]string{
        "name": html.EscapeString(name),
    }
    _ = json.NewEncoder(w).Encode(out)
}
