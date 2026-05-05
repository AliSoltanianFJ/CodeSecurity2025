package main

import (
    "net/http"
    "html"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...
    yamlPayload := r.URL.Query().Get("payload")
    // ...existing code...

    const maxPayloadLen = 10 * 1024 // 10 KiB limit to avoid large input attacks
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    if len(yamlPayload) > maxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Decode into a fixed, minimal struct to avoid insecure deserialization.
    var data struct {
        Name string `yaml:"name"`
    }

    if err := yaml.Unmarshal([]byte(yamlPayload), &data); err != nil {
        http.Error(w, "invalid YAML", http.StatusBadRequest)
        return
    }

    // Basic validation and sanitization of the name field.
    name := strings.TrimSpace(data.Name)
    const maxNameLen = 200
    if name == "" || len(name) > maxNameLen {
        http.Error(w, "invalid name", http.StatusBadRequest)
        return
    }

    // Allow letters, numbers, whitespace and a small set of safe punctuation.
    var validName = regexp.MustCompile(`^[\p{L}\p{N}\s._-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid name characters", http.StatusBadRequest)
        return
    }

    // Escape output to prevent XSS and respond with only the sanitized name.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(html.EscapeString(name)))
}
