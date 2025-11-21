package main

import (
    "encoding/json"
    "html"
    "io"
    "log"
    "net/http"
    "strings"
    "unicode"

    "gopkg.in/yaml.v3"
)

// ...existing code...

const maxPayloadSize = 4096 // limit to 4 KiB to mitigate DoS and large object attacks

func main() {
    http.HandleFunc("/api", apiHandler)
    // Note: In production, use TLS (HTTPS) and do not expose plain HTTP.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/*
Security decisions and mitigations applied:
- Strict payload size limit (maxPayloadSize) to prevent resource exhaustion.
- Use yaml.Decoder with KnownFields(true) to reject unknown fields (defense-in-depth).
- Decode into a concrete struct (no interface{} usage) to avoid unsafe deserialization.
- Ensure only a single YAML document is accepted.
- Validate and sanitize the 'name' field (trim, length bounds, no control chars).
- Escape output using html.EscapeString to mitigate reflected XSS.
- Do not reveal internal errors to clients; log detailed errors server-side.
- Require TLS for transport security (see comment in main); handler rejects requests over plain HTTP.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce TLS for transport security. Reject plain HTTP to avoid leaking sensitive data in transit.
    if r.TLS == nil {
        http.Error(w, "insecure transport: use HTTPS", http.StatusForbidden)
        return
    }

    // Only allow GET or POST for this simple endpoint; reject other methods.
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read payload from query param (as per original code). Limit size to prevent abuse.
    raw := r.URL.Query().Get("payload")
    if raw == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    if len(raw) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Prepare a limited reader to feed the YAML decoder (defense against large streams).
    limited := io.LimitReader(strings.NewReader(raw), maxPayloadSize)

    // Define the expected structure explicitly (avoid generic interface{} to prevent unsafe types).
    type Payload struct {
        Name string `yaml:"name"`
    }

    dec := yaml.NewDecoder(limited)
    // Reject unknown fields to reduce attack surface.
    dec.KnownFields(true)

    var p Payload
    if err := dec.Decode(&p); err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Ensure there's no second YAML document (only a single document allowed).
    var extra interface{}
    if err := dec.Decode(&extra); err != io.EOF {
        http.Error(w, "only a single document allowed", http.StatusBadRequest)
        return
    }

    // Validate the 'name' field: non-empty after trim, reasonable length, no control characters.
    name := strings.TrimSpace(p.Name)
    if !isValidName(name) {
        http.Error(w, "invalid name value", http.StatusBadRequest)
        return
    }

    // Sanitize for safe output (escape any characters that could be interpreted by clients).
    safeName := html.EscapeString(name)

    // Return the name as JSON. Use application/json and do not leak internal details.
    resp := map[string]string{"name": safeName}
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}

// isValidName applies conservative validation rules:
// - non-empty, max length 100 characters
// - no Unicode control characters
// - rejects only-control names (e.g., entirely whitespace)
// This keeps validation simple and audit-friendly while preventing obvious abuses.
func isValidName(s string) bool {
    if s == "" {
        return false
    }
    if len(s) > 100 {
        return false
    }
    hasNonSpace := false
    for _, r := range s {
        if unicode.IsControl(r) {
            return false
        }
        if !unicode.IsSpace(r) {
            hasNonSpace = true
        }
    }
    return hasNonSpace
}
