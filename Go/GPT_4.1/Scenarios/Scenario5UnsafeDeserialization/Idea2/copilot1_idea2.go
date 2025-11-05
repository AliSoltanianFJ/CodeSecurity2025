package main

import (
    "encoding/json"
    "html"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// Struct with only expected fields to prevent insecure deserialization (CWE-502)
type Payload struct {
    Name string `yaml:"name"`
}

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    // Input validation: check payload length and content
    if len(yamlPayload) == 0 || len(yamlPayload) > 2048 {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    if strings.ContainsAny(yamlPayload, "<>{}") {
        http.Error(w, "Invalid characters in payload", http.StatusBadRequest)
        return
    }

    var data Payload
    // Strict YAML decoding to prevent unknown fields
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true)
    if err := decoder.Decode(&data); err != nil {
        http.Error(w, "Failed to parse payload", http.StatusBadRequest)
        return
    }

    // Sanitize output to prevent XSS
    safeName := html.EscapeString(strings.TrimSpace(data.Name))

    // Return as JSON for safety and clarity
    resp := map[string]string{"name": safeName}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}
