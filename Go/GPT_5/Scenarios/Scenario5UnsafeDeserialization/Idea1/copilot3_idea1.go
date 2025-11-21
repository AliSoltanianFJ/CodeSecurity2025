package main

import (
    "fmt"
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
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload query parameter", http.StatusBadRequest)
        return
    }

    // Basic size limit to avoid expensive processing
    if len(yamlPayload) > 8*1024 { // 8 KB max
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Decode into a strict struct and reject unknown fields
    var payload struct {
        Name string `yaml:"name"`
    }

    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    dec.KnownFields(true) // reject unexpected fields/tags

    if err := dec.Decode(&payload); err != nil {
        http.Error(w, "invalid or unsafe yaml: "+err.Error(), http.StatusBadRequest)
        return
    }

    // Validate the extracted name: non-empty, reasonable length, valid UTF-8
    name := strings.TrimSpace(payload.Name)
    if name == "" {
        http.Error(w, "name field is empty or missing", http.StatusBadRequest)
        return
    }
    if !utf8.ValidString(name) || utf8.RuneCountInString(name) > 256 {
        http.Error(w, "invalid name value", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, name)
}
// ...existing code...
