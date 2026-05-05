package main

import (
    "html"
    "net/http"
    "strings"
    "unicode"
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
    // Read and validate raw input
    yamlPayload := strings.TrimSpace(r.URL.Query().Get("payload"))
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Enforce a conservative maximum size to prevent large payload processing
    const maxPayloadBytes = 2048
    if len(yamlPayload) > maxPayloadBytes {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Safe target struct: prevents arbitrary object construction during decode
    type safePayload struct {
        Name string `yaml:"name"`
    }

    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    dec.KnownFields(true) // reject unknown/unexpected fields to reduce attack surface

    var p safePayload
    if err := dec.Decode(&p); err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate the "name" field: non-empty, reasonable length, and allowed characters only
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "missing name", http.StatusBadRequest)
        return
    }
    if utf8.RuneCountInString(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    for _, r := range name {
        if unicode.IsLetter(r) || unicode.IsDigit(r) || r == ' ' || r == '-' || r == '_' || r == '.' || r == '\'' || r == ',' {
            continue
        }
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Prevent XSS by escaping before writing to response
    safeName := html.EscapeString(name)

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    _, _ = w.Write([]byte(safeName))
}
