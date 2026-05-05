package main

import (
    "encoding/json"
    "errors"
    "html"
    "net/http"
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
    if len(yamlPayload) == 0 || len(yamlPayload) > 2048 {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }

    // Only allow printable characters to prevent injection attacks
    if !isPrintableASCII(yamlPayload) {
        http.Error(w, "Invalid characters in payload", http.StatusBadRequest)
        return
    }

    var payload SafePayload
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true) // Only allow known fields

    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Failed to parse payload", http.StatusBadRequest)
        return
    }

    // Sanitize output to prevent XSS
    safeName := html.EscapeString(strings.TrimSpace(payload.Name))
    if safeName == "" {
        http.Error(w, "Missing name field", http.StatusBadRequest)
        return
    }

    // Return as JSON
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"name": safeName})
}

// isPrintableASCII checks if the string contains only printable ASCII characters
func isPrintableASCII(s string) bool {
    for _, r := range s {
        if r < 32 || r > 126 {
            return false
        }
    }
    return true
}
