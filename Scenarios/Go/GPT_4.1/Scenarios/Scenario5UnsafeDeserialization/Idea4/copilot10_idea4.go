package main

import (
    "encoding/json"
    "errors"
    "fmt"
    "gopkg.in/yaml.v3" // Secure YAML library, avoids unsafe deserialization
    "io"
    "net/http"
    "regexp"
    "strings"
)

// Struct for strict YAML decoding. Only expected fields are defined.
type Payload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Enforce HTTPS in production; ListenAndServeTLS should be used with valid certs.
    // For demo, HTTP is used. Never use HTTP in production.
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload and returns the "name" field.
Security measures:
- Only accepts POST requests with Content-Type: application/x-yaml or application/yaml.
- Limits request body size to prevent DoS.
- Uses strict YAML decoding to avoid unsafe deserialization (CWE-502).
- Validates and sanitizes the "name" field to prevent injection attacks (CWE-20, CWE-79).
- Handles errors without leaking sensitive info.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow POST requests
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce Content-Type
    ct := r.Header.Get("Content-Type")
    if !strings.HasPrefix(ct, "application/x-yaml") && !strings.HasPrefix(ct, "application/yaml") {
        http.Error(w, "Unsupported Media Type", http.StatusUnsupportedMediaType)
        return
    }

    // Limit body size to 1KB to prevent DoS attacks
    const maxBodySize = 1024
    r.Body = http.MaxBytesReader(w, r.Body, maxBodySize)
    defer r.Body.Close()

    body, err := io.ReadAll(r.Body)
    if err != nil {
        http.Error(w, "Invalid request body", http.StatusBadRequest)
        return
    }

    // Strict YAML decoding: only known fields are allowed
    var payload Payload
    decoder := yaml.NewDecoder(strings.NewReader(string(body)))
    decoder.KnownFields(true) // Reject unknown fields

    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Malformed YAML payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the "name" field
    name, err := validateAndSanitizeName(payload.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Respond with JSON (safe output encoding)
    resp := map[string]string{"name": name}
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    json.NewEncoder(w).Encode(resp)
}

// validateAndSanitizeName ensures the name is safe and well-formed.
// Only allows letters, numbers, spaces, hyphens, and underscores. Max length 100.
func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("name must be 1-100 characters")
    }
    // Allow only safe characters
    validName := regexp.MustCompile(`^[a-zA-Z0-9 _-]+$`)
    if !validName.MatchString(name) {
        return "", errors.New("name contains invalid characters")
    }
    return name, nil
}
