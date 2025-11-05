package main

import (
    "encoding/json"
    "errors"
    "html"
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

type SafePayload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    // Input validation: check length and basic structure
    if len(yamlPayload) == 0 || len(yamlPayload) > 1024 {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }

    // Only allow safe YAML characters (letters, numbers, spaces, basic punctuation)
    safeYAML := regexp.MustCompile(`^[\w\s\-:{}",']+$`)
    if !safeYAML.MatchString(yamlPayload) {
        http.Error(w, "Payload contains invalid characters", http.StatusBadRequest)
        return
    }

    var payload SafePayload

    // Use yaml.v3 Decoder with strict mode to prevent unknown fields
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true)
    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Failed to parse YAML", http.StatusBadRequest)
        return
    }

    // Sanitize the name field to prevent XSS
    safeName := html.EscapeString(strings.TrimSpace(payload.Name))

    // Validate the name field (e.g., only allow letters, numbers, spaces, and limit length)
    if len(safeName) == 0 || len(safeName) > 100 {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }
    validName := regexp.MustCompile(`^[\w\s\-']+$`)
    if !validName.MatchString(safeName) {
        http.Error(w, "Name contains invalid characters", http.StatusBadRequest)
        return
    }

    // Return the sanitized name as JSON
    resp := map[string]string{"name": safeName}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}
