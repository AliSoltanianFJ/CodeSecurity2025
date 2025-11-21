package main

import (
    "fmt"
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

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // basic size limit to mitigate resource exhaustion
    if len(yamlPayload) > 10*1024 {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // strict schema: only allow the "name" field and reject any unknown fields
    var payload struct {
        Name string `yaml:"name"`
    }

    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    dec.KnownFields(true) // reject unknown/disallowed fields
    if err := dec.Decode(&payload); err != nil {
        http.Error(w, "invalid or disallowed yaml: "+err.Error(), http.StatusBadRequest)
        return
    }

    if payload.Name == "" {
        http.Error(w, "name missing or empty", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    // escape before returning to avoid reflected XSS if consumed by browsers
    fmt.Fprint(w, html.EscapeString(payload.Name))
}
// ...existing code...
