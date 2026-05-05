package main

import (
    "fmt"
    "html"
    "io"
    "net/http"
    "strings"
    "unicode/utf8"

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
    // - Limit payload size
    // - Only unmarshal into a strict typed struct (prevents insecure deserialization)
    // - Validate and sanitize output to prevent XSS
    const maxPayloadSize = 2048 // bytes

    var payload string

    // Prefer query param but accept small POST/PUT body as alternative
    payload = r.URL.Query().Get("payload")

    if payload == "" && (r.Method == http.MethodPost || r.Method == http.MethodPut) {
        // Limit body read to avoid large payloads
        r.Body = http.MaxBytesReader(w, r.Body, maxPayloadSize)
        b, err := io.ReadAll(r.Body)
        if err != nil {
            http.Error(w, "invalid request body", http.StatusBadRequest)
            return
        }
        payload = string(b)
    }

    payload = strings.TrimSpace(payload)
    if payload == "" || len(payload) > maxPayloadSize {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Strictly-typed target to avoid arbitrary types during deserialization
    var in struct {
        Name string `yaml:"name"`
    }

    if err := yaml.Unmarshal([]byte(payload), &in); err != nil {
        http.Error(w, "invalid YAML", http.StatusBadRequest)
        return
    }

    name := strings.TrimSpace(in.Name)
    if name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }
    if utf8.RuneCountInString(name) > 200 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    // Escape to prevent XSS when reflecting user input
    safeName := html.EscapeString(name)

    // Security headers to reduce attack surface
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    w.WriteHeader(http.StatusOK)
    fmt.Fprint(w, safeName)
}
// ...existing code...
