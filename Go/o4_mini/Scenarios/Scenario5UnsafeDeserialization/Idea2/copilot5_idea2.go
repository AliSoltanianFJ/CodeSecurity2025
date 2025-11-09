package main

import (
    "fmt"
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

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    // ...existing code...

    // Basic trimming and size check to prevent extremely large inputs
    yamlPayload = strings.TrimSpace(yamlPayload)
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    const maxPayloadSize = 4 * 1024 // 4 KB
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Define a strict target struct to avoid unsafe deserialization
    type requestData struct {
        Name string `yaml:"name"`
    }

    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    // Reject unknown fields to reduce attack surface
    dec.KnownFields(true)

    var req requestData
    if err := dec.Decode(&req); err != nil {
        http.Error(w, "invalid yaml", http.StatusBadRequest)
        return
    }

    name := strings.TrimSpace(req.Name)
    if name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    // Validate allowed characters: letters, numbers, whitespace, hyphen, underscore, dot
    validName := regexp.MustCompile(`^[\p{L}\p{N}\s\-\._]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Escape output to prevent XSS
    safeName := html.EscapeString(name)

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprintln(w, safeName)
}
// ...existing code...
