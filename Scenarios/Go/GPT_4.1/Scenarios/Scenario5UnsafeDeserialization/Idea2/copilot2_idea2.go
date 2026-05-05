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

    // Input validation: check for empty payload and reasonable length
    if yamlPayload == "" || len(yamlPayload) > 2048 {
        http.Error(w, "Invalid or missing payload", http.StatusBadRequest)
        return
    }

    // Only allow printable characters to avoid injection attacks
    if !isPrintableASCII(yamlPayload) {
        http.Error(w, "Payload contains invalid characters", http.StatusBadRequest)
        return
    }

    var payload SafePayload

    // Use yaml.v3 decoder (safe for simple structs, avoids known unsafe tags)
    err := yaml.Unmarshal([]byte(yamlPayload), &payload)
    if err != nil {
        http.Error(w, "Failed to parse YAML", http.StatusBadRequest)
        return
    }

    // Sanitize output to prevent XSS
    safeName := html.EscapeString(strings.TrimSpace(payload.Name))

    // Respond with JSON
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
