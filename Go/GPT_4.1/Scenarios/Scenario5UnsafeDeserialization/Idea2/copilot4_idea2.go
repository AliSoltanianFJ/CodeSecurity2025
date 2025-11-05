package main

import (
    "encoding/json"
    "errors"
    "html"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

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
    if yamlPayload == "" {
        http.Error(w, "Missing payload parameter", http.StatusBadRequest)
        return
    }

    // Limit payload size to prevent DoS attacks
    if len(yamlPayload) > 2048 {
        http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Reject suspicious input (basic check)
    if strings.ContainsAny(yamlPayload, "!&*|>") {
        http.Error(w, "Invalid characters in payload", http.StatusBadRequest)
        return
    }

    var p Payload
    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    dec.KnownFields(true) // Only allow known fields

    if err := dec.Decode(&p); err != nil {
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the name field
    name, err := validateAndSanitizeName(p.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Return as JSON to avoid XSS
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"name": name})
}

func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("invalid length")
    }
    // Allow only letters, numbers, spaces, hyphens, and underscores
    for _, r := range name {
        if !(r == '-' || r == '_' || r == ' ' ||
            (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9')) {
            return "", errors.New("invalid character")
        }
    }
    // Escape HTML to prevent XSS
    return html.EscapeString(name), nil
}
