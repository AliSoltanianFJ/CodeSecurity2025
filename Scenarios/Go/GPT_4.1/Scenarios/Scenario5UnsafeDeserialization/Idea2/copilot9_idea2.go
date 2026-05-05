package main

import (
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// Define a strict struct for expected YAML input
type Payload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    // Input validation: check for empty payload and reasonable length
    if yamlPayload == "" || len(yamlPayload) > 1024 {
        http.Error(w, "Invalid or missing payload", http.StatusBadRequest)
        return
    }

    // Only allow printable characters to prevent injection attacks
    if !isPrintableASCII(yamlPayload) {
        http.Error(w, "Payload contains invalid characters", http.StatusBadRequest)
        return
    }

    var payload Payload

    // Strict YAML decoding to prevent insecure deserialization
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true) // Only allow known fields

    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Failed to parse YAML payload", http.StatusBadRequest)
        return
    }

    // Sanitize output to prevent XSS
    safeName := html.EscapeString(payload.Name)

    // Return as JSON for safety and clarity
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"name": safeName})
}

// Helper to check for printable ASCII characters only
func isPrintableASCII(s string) bool {
    for _, r := range s {
        if r < 32 || r > 126 {
            return false
        }
    }
    return true
}
