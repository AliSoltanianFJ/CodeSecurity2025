package main

import (
    "net/http"
    "io"
    "strings"
    "html"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    const maxPayload = 2048 // limit to prevent large payloads
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    if len(yamlPayload) > maxPayload {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Use a limited reader and a strict YAML decoder to avoid insecure deserialization
    rdr := io.LimitReader(strings.NewReader(yamlPayload), maxPayload)
    dec := yaml.NewDecoder(rdr)
    dec.KnownFields(true) // reject unknown fields

    var data struct {
        Name string `yaml:"name"`
    }
    if err := dec.Decode(&data); err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize extracted field
    name := strings.TrimSpace(data.Name)
    if name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    safeName := html.EscapeString(name) // prevent XSS
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    _, _ = w.Write([]byte(safeName))
}
// ...existing code...
