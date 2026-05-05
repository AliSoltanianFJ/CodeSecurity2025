package main

import (
    "html"
    "io"
    "net/http"
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
    // Validate HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    yamlPayload := r.URL.Query().Get("payload")

    // Basic input validation
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    const maxPayloadSize = 4 * 1024 // 4 KiB
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Define a strict, minimal struct for safe deserialization (prevents insecure deserialization)
    var in struct {
        Name string `yaml:"name"`
    }

    // Use strict unmarshal to reject unknown fields
    if err := yaml.UnmarshalStrict([]byte(yamlPayload), &in); err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Sanitize and validate the name field
    name := strings.TrimSpace(in.Name)
    if name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }

    // Limit displayed name length
    const maxNameLen = 256
    if len(name) > maxNameLen {
        name = name[:maxNameLen]
    }

    // Remove control characters that could lead to header/CRLF injection and normalise whitespace
    name = strings.ReplaceAll(name, "\r", " ")
    name = strings.ReplaceAll(name, "\n", " ")
    name = strings.TrimSpace(name)

    // Escape to prevent XSS if the response is rendered in a browser; use text/plain content type as additional protection
    name = html.EscapeString(name)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Write sanitized output
    _, _ = io.WriteString(w, name)
}
// ...existing code...
