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
    // Validate method
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    yamlPayload := r.URL.Query().Get("payload")
    // Validate input presence
    yamlPayload = strings.TrimSpace(yamlPayload)
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Limit payload size to prevent resource exhaustion
    const maxPayloadSize = 4096
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Use a strict, safe struct for deserialization to avoid insecure deserialization
    type requestData struct {
        Name string `yaml:"name"`
    }
    var data requestData

    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    // Reject unknown fields to avoid unexpected data
    dec.KnownFields(true)
    if err := dec.Decode(&data); err != nil {
        http.Error(w, "invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Sanitize and validate the name field
    data.Name = strings.TrimSpace(data.Name)
    if data.Name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }
    if len(data.Name) > 200 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    // Escape output to prevent cross-site scripting and return as plain text
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    _, _ = w.Write([]byte(html.EscapeString(data.Name)))
}
