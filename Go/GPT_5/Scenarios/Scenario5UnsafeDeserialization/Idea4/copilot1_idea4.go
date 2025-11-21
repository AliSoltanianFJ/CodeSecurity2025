// ...existing code...
package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "strings"
    "unicode"

    "gopkg.in/yaml.v3"
)

// Security notes (summary):
// - Require TLS (reject non-TLS requests) to protect data in transit.
// - Enforce strict input validation: size limit, allowed characters, reject YAML tags/anchors.
// - Prevent insecure deserialization by only allowing a single known key ("name") and unmarshalling
//   into a simple, typed struct. Additional keys are rejected.
// - Do not log or echo raw payloads. Return minimal, safe error messages.
// - Output encoded JSON to avoid XSS when consumed by browsers.
// - In production, run behind a properly configured TLS endpoint or use ListenAndServeTLS with valid certs.

func main() {
    http.HandleFunc("/api", apiHandler)
    // NOTE: In production, serve via TLS (ListenAndServeTLS) with valid certificates. This
    // example uses ListenAndServe for local testing only.
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Require HTTPS/TLS. Reject plain HTTP to avoid transmitting sensitive data in clear text.
    if r.TLS == nil {
        http.Error(w, "TLS required; use HTTPS", http.StatusUpgradeRequired) // 426
        return
    }

    // Read and validate the 'payload' query parameter.
    payload := r.URL.Query().Get("payload")
    payload = strings.TrimSpace(payload)
    if payload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Enforce a strict maximum size for the input to mitigate resource exhaustion.
    const maxPayloadBytes = 10 * 1024 // 10 KB
    if len(payload) > maxPayloadBytes {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Reject binary/control characters except common whitespace (tab, newline, carriage return).
    for _, rch := range payload {
        if rch == '\n' || rch == '\r' || rch == '\t' {
            continue
        }
        if rch == '\u0000' || unicode.IsControl(rch) {
            http.Error(w, "payload contains disallowed characters", http.StatusBadRequest)
            return
        }
    }

    // Disallow YAML features that can be abused for unexpected behavior:
    // - explicit tags ("!!"), anchors (&), aliases (*), merge keys (<<), or execution-like tags (!)
    lower := strings.ToLower(payload)
    if strings.Contains(lower, "!!") || strings.Contains(payload, "&") || strings.Contains(payload, "*") ||
        strings.Contains(payload, "<<") || strings.Contains(payload, "!") {
        http.Error(w, "payload contains disallowed YAML constructs", http.StatusBadRequest)
        return
    }

    // First, unmarshal into a generic map so we can enforce an allow-list of keys.
    var rawMap map[string]interface{}
    if err := yaml.Unmarshal([]byte(payload), &rawMap); err != nil {
        // Do not reveal internal parser errors to the client.
        http.Error(w, "invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Only allow the single "name" key at the top level. Reject if other keys present.
    if len(rawMap) == 0 {
        http.Error(w, "payload must contain 'name'", http.StatusBadRequest)
        return
    }
    if len(rawMap) != 1 {
        http.Error(w, "only 'name' field is allowed", http.StatusBadRequest)
        return
    }
    if _, ok := rawMap["name"]; !ok {
        http.Error(w, "payload must contain 'name' field", http.StatusBadRequest)
        return
    }

    // Now unmarshal into a tightly-typed struct. This avoids deserializing arbitrary types.
    type Payload struct {
        Name string `yaml:"name" json:"name"`
    }
    var p Payload
    if err := yaml.Unmarshal([]byte(payload), &p); err != nil {
        http.Error(w, "failed to parse payload", http.StatusBadRequest)
        return
    }

    // Validate the 'name' field strictly.
    p.Name = strings.TrimSpace(p.Name)
    if p.Name == "" {
        http.Error(w, "name must be non-empty", http.StatusBadRequest)
        return
    }
    const maxNameLen = 200
    if len(p.Name) > maxNameLen {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Disallow control characters in name.
    for _, rch := range p.Name {
        if unicode.IsControl(rch) {
            http.Error(w, "name contains disallowed characters", http.StatusBadRequest)
            return
        }
    }

    // Escape to prevent reflected XSS if this value is displayed in a browser later.
    safeName := html.EscapeString(p.Name)

    // Return the name as JSON with safe encoding and proper Content-Type.
    resp := map[string]string{"name": safeName}
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    enc, err := json.Marshal(resp)
    if err != nil {
        // Do not leak internal error details.
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    _, _ = w.Write(enc)

    // Minimal server-side logging for monitoring; do not log payload contents.
    fmt.Println("served /api request successfully")
}
