// ...existing code...
package main

import (
    "encoding/json"
    "log"
    "net/http"
    "strings"
    "unicode"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Validate and fetch input
    payload := strings.TrimSpace(r.URL.Query().Get("payload"))
    if payload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Reject excessively large payloads to avoid resource exhaustion
    const maxPayloadLen = 4096
    if len(payload) > maxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Strictly decode YAML into a simple, known struct to avoid insecure deserialization.
    // Do NOT decode into interface{} or allow custom tags/types.
    var in struct {
        Name string `yaml:"name"`
    }
    if err := yaml.UnmarshalStrict([]byte(payload), &in); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the name field
    name := strings.TrimSpace(in.Name)
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    // Reject names that are too long or contain control characters.
    const maxNameRunes = 200
    if len([]rune(name)) > maxNameRunes {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    for _, r := range name {
        if unicode.IsControl(r) {
            http.Error(w, "invalid characters in name", http.StatusBadRequest)
            return
        }
    }

    // Return a safe JSON response (encoding/json will properly escape content).
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    resp := map[string]string{"name": name}
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // extra protection against XSS
    if err := enc.Encode(resp); err != nil {
        // Generic error, don't leak internal details
        http.Error(w, "failed to write response", http.StatusInternalServerError)
        return
    }
}
// ...existing code...
