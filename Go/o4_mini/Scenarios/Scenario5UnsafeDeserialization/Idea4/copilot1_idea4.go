package main

import (
    "fmt"
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

// ...existing code...

func main() {
    http.HandleFunc("/api", apiHandler)
    // NOTE: in production, serve behind TLS (reverse proxy) and enable HSTS.
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (defense-in-depth)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Read payload from query (kept for compatibility). Prefer request body in production.
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Protect against very large payloads to mitigate resource exhaustion
    const maxPayloadBytes = 4 * 1024 // 4 KiB
    if len(yamlPayload) > maxPayloadBytes {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Define a strict, whitelisted schema to avoid arbitrary type creation
    type Payload struct {
        Name string `yaml:"name"`
    }

    var p Payload
    // Unmarshal into the safe struct only (no interfaces, no dynamic types)
    if err := yaml.Unmarshal([]byte(yamlPayload), &p); err != nil {
        // Return a generic error message to avoid leaking implementation details
        http.Error(w, "invalid yaml", http.StatusBadRequest)
        return
    }

    // Canonicalize and validate the 'name' field using a conservative whitelist
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }

    // Allow Unicode letters/numbers and a small set of safe punctuation.
    // Limit length to avoid abuse.
    var validName = regexp.MustCompile(`^[\p{L}\p{N} _\-.]{1,256}$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid name", http.StatusBadRequest)
        return
    }

    // Successful response: return the canonical name only (no extra metadata)
    fmt.Fprintln(w, name)
}

// ...existing code...
