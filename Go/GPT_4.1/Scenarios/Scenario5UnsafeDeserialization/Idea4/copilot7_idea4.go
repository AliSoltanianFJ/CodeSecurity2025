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

// Security: Only allow expected fields, use strict struct for deserialization
type NamePayload struct {
    Name string `yaml:"name" json:"name"`
}

// Security: Limit payload size to prevent DoS attacks
const maxPayloadSize = 4096 // 4KB

func main() {
    // Security: Always use HTTPS in production to protect data in transit
    http.HandleFunc("/api", apiHandler)
    // Security: ListenAndServeTLS should be used in production with valid certs
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload and returns the "name" field.
- Only accepts POST requests with Content-Type: application/x-yaml or application/yaml.
- Limits payload size.
- Uses strict struct for deserialization to prevent unsafe deserialization (CWE-502).
- Validates and sanitizes the "name" field.
- Handles errors without leaking sensitive info.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    ct := r.Header.Get("Content-Type")
    if !strings.HasPrefix(ct, "application/x-yaml") && !strings.HasPrefix(ct, "application/yaml") {
        http.Error(w, "Unsupported Media Type", http.StatusUnsupportedMediaType)
        return
    }

    // Security: Limit payload size to prevent resource exhaustion
    r.Body = http.MaxBytesReader(w, r.Body, maxPayloadSize)
    defer r.Body.Close()
    body, err := io.ReadAll(r.Body)
    if err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }

    var payload NamePayload
    // Security: Use strict struct, do not allow unknown fields
    decoder := yaml.NewDecoder(strings.NewReader(string(body)))
    decoder.KnownFields(true)
    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Malformed YAML", http.StatusBadRequest)
        return
    }

    // Security: Validate and sanitize the "name" field
    name, err := validateAndSanitizeName(payload.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Security: Respond with JSON to avoid content sniffing/XSS
    w.Header().Set("Content-Type", "application/json")
    resp := map[string]string{"name": name}
    json.NewEncoder(w).Encode(resp)
}

// validateAndSanitizeName checks the name for allowed characters and length.
// Only allows letters, numbers, spaces, hyphens, and underscores. Max 100 chars.
func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("name must be 1-100 characters")
    }
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
