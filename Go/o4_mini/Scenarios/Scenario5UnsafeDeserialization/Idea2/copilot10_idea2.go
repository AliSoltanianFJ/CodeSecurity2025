package main

import (
    "html"
    "net/http"
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
    // Validate HTTP method
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    yamlPayload := r.URL.Query().Get("payload")
    // Input validation: presence and size limit
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    const MaxPayloadLen = 4096
    if len(yamlPayload) > MaxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Strict deserialization into a known-safe struct to avoid insecure deserialization
    type Payload struct {
        Name string `yaml:"name"`
    }
    var p Payload
    if err := yaml.UnmarshalStrict([]byte(yamlPayload), &p); err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the name field
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name missing or empty", http.StatusBadRequest)
        return
    }
    const MaxNameLen = 256
    if len(name) > MaxNameLen {
        name = name[:MaxNameLen]
    }
    safeName := html.EscapeString(name)

    // Return as plain text (escaped) to prevent XSS
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    _, _ = w.Write([]byte(safeName))
}