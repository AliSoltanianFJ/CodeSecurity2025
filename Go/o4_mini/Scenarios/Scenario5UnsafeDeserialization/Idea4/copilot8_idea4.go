// ...existing code...
package main

import (
    "encoding/json"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (defense-in-depth)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    // Read and validate input
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, `{"error":"missing payload"}`, http.StatusBadRequest)
        return
    }

    // Enforce size limit to mitigate resource exhaustion
    const maxPayloadBytes = 4 * 1024 // 4 KB
    if len(payload) > maxPayloadBytes {
        http.Error(w, `{"error":"payload too large"}`, http.StatusRequestEntityTooLarge)
        return
    }

    // Canonicalize input
    payload = strings.TrimSpace(payload)

    // Strict YAML decode into a small schema to avoid unsafe deserialization
    type requestYAML struct {
        Name string `yaml:"name"`
    }
    var req requestYAML

    decoder := yaml.NewDecoder(strings.NewReader(payload))
    // Reject unknown fields to avoid attackers injecting unexpected structure
    decoder.KnownFields(true)
    if err := decoder.Decode(&req); err != nil {
        // Do not leak internal error details to clients
        log.Printf("yaml decode error: %v", err)
        http.Error(w, `{"error":"invalid yaml payload"}`, http.StatusBadRequest)
        return
    }

    // Validate 'name' with a strict whitelist (letters, numbers, space, basic punctuation)
    // Length limit enforced here as well.
    const maxNameLen = 100
    if req.Name == "" {
        http.Error(w, `{"error":"name is required"}`, http.StatusBadRequest)
        return
    }
    if len(req.Name) > maxNameLen {
        http.Error(w, `{"error":"name too long"}`, http.StatusBadRequest)
        return
    }
    // Whitelist pattern: allow common name characters
    var validName = regexp.MustCompile(`^[A-Za-z0-9 \-'\.\,]+$`)
    if !validName.MatchString(req.Name) {
        http.Error(w, `{"error":"invalid characters in name"}`, http.StatusBadRequest)
        return
    }

    // Output encoding: escape to avoid injection in logs/HTML contexts and marshal as JSON
    escapedName := html.EscapeString(req.Name)
    out := map[string]string{"name": escapedName}
    enc, err := json.Marshal(out)
    if err != nil {
        log.Printf("json marshal error: %v", err)
        http.Error(w, `{"error":"internal error"}`, http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    _, _ = w.Write(enc)
}
// ...existing code...
