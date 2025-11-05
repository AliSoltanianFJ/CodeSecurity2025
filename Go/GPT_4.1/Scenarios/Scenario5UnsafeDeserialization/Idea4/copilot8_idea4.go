package main

import (
    "encoding/json"
    "errors"
    "io"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// Secure struct for strict YAML decoding
type NamePayload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Enforce HTTPS in production; for demo, log a warning if not using TLS
    // http.ListenAndServeTLS(":8080", "cert.pem", "key.pem", nil)
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload and returns the "name" field.
- Only accepts POST requests with Content-Type: application/x-yaml or text/yaml.
- Limits payload size to prevent DoS attacks.
- Uses strict YAML decoding to avoid unsafe deserialization (CWE-502).
- Validates and sanitizes the "name" field to prevent injection/XSS.
- Does not leak internal errors to the client.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST requests
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce Content-Type
    ct := r.Header.Get("Content-Type")
    if !strings.HasPrefix(ct, "application/x-yaml") && !strings.HasPrefix(ct, "text/yaml") {
        http.Error(w, "Unsupported Media Type", http.StatusUnsupportedMediaType)
        return
    }

    // Limit payload size to 1KB to prevent DoS
    const maxPayloadSize = 1024
    body := http.MaxBytesReader(w, r.Body, maxPayloadSize)
    defer body.Close()

    var payload NamePayload
    decoder := yaml.NewDecoder(body)
    decoder.KnownFields(true) // Only allow known fields

    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the "name" field
    name, err := validateAndSanitizeName(payload.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Respond with JSON (safe encoding)
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    json.NewEncoder(w).Encode(map[string]string{"name": name})
}

// validateAndSanitizeName checks that the name is safe and well-formed.
// - Only allows letters, numbers, spaces, hyphens, and underscores.
// - Limits length to 100 characters.
func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("name must be 1-100 characters")
    }
    for _, r := range name {
        if !(r == '-' || r == '_' || r == ' ' ||
            (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9')) {
            return "", errors.New("name contains invalid characters")
        }
    }
    return name, nil
}
