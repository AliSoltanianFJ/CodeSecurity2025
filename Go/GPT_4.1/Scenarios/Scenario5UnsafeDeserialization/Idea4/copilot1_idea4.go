package main

import (
    "encoding/json"
    "errors"
    "fmt"
    "io"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// Security: Only allow expected fields and types during deserialization.
// Avoid using yaml.Unmarshal directly into interface{} or map[string]interface{} to prevent unsafe deserialization (CWE-502).
// Use strict decoding and input size limits to prevent DoS and resource exhaustion (CWE-400).
// Never log or return raw errors to the client (CWE-209).

// Person is a strict struct for expected YAML input.
type Person struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Security: Always use HTTPS in production to protect data in transit (CWE-319).
    // For demo purposes, this uses HTTP. In production, use ListenAndServeTLS.
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only accept POST requests with Content-Type: application/x-yaml or text/yaml.
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }
    ct := r.Header.Get("Content-Type")
    if !strings.HasPrefix(ct, "application/x-yaml") && !strings.HasPrefix(ct, "text/yaml") {
        http.Error(w, "Unsupported Media Type", http.StatusUnsupportedMediaType)
        return
    }

    // Security: Limit request body size to prevent DoS (CWE-400).
    const maxBodySize = 4096 // 4KB
    body := http.MaxBytesReader(w, r.Body, maxBodySize)
    defer body.Close()

    // Read the YAML payload.
    payload, err := io.ReadAll(body)
    if err != nil {
        http.Error(w, "Invalid request body", http.StatusBadRequest)
        return
    }

    // Security: Use yaml.Decoder with KnownFields to reject unknown fields (CWE-915).
    var person Person
    decoder := yaml.NewDecoder(strings.NewReader(string(payload)))
    decoder.KnownFields(true)
    if err := decoder.Decode(&person); err != nil {
        http.Error(w, "Malformed YAML or unexpected fields", http.StatusBadRequest)
        return
    }

    // Security: Validate and sanitize the "name" field.
    name, err := validateName(person.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Security: Respond with JSON, set safe headers.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "no-store")
    resp := map[string]string{"name": name}
    json.NewEncoder(w).Encode(resp)
}

// validateName checks that the name is non-empty, reasonable length, and contains only safe characters.
func validateName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("name must be 1-100 characters")
    }
    // Allow only letters, numbers, spaces, hyphens, and underscores.
    for _, r := range name {
        if !(r == ' ' || r == '-' || r == '_' ||
            (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9')) {
            return "", errors.New("name contains invalid characters")
        }
    }
    return name, nil
}
