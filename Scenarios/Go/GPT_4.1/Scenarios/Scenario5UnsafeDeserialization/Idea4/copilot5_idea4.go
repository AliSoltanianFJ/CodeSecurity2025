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

// Security: Only allow deserialization into a strict, minimal struct.
// Avoids CWE-502 (Deserialization of Untrusted Data) and CWE-915 (Improper Neutralization of Data During Deserialization).
type safePayload struct {
    Name string `yaml:"name" json:"name"`
}

// Security: Limit payload size to prevent DoS attacks (CWE-400).
const maxPayloadSize = 4096 // 4KB

func main() {
    // Security: Always use HTTPS in production to protect data in transit (CWE-319).
    // For demo purposes, ListenAndServeTLS is not used here.
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload from the "payload" query parameter,
validates it, and returns the "name" field as JSON.

Security measures:
- Strictly limits deserialization to a known struct (safePayload).
- Validates and sanitizes all user input.
- Limits payload size to prevent resource exhaustion.
- Does not leak internal errors to the client.
- Uses context-aware output encoding (JSON).
- Does not log or expose sensitive data.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only accept POST requests for payloads to avoid leaking sensitive data in URLs.
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS.
    r.Body = http.MaxBytesReader(w, r.Body, maxPayloadSize)
    defer r.Body.Close()

    body, err := io.ReadAll(r.Body)
    if err != nil {
        http.Error(w, "Invalid request", http.StatusBadRequest)
        return
    }

    // Security: Reject empty payloads.
    if len(body) == 0 {
        http.Error(w, "Empty payload", http.StatusBadRequest)
        return
    }

    // Security: Only allow YAML content type.
    contentType := r.Header.Get("Content-Type")
    if !strings.HasPrefix(contentType, "application/x-yaml") && !strings.HasPrefix(contentType, "text/yaml") {
        http.Error(w, "Unsupported Content-Type", http.StatusUnsupportedMediaType)
        return
    }

    var payload safePayload

    // Security: Use yaml.v3 decoder with strict mode to prevent unknown fields (CWE-915).
    decoder := yaml.NewDecoder(strings.NewReader(string(body)))
    decoder.KnownFields(true)
    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Malformed YAML payload", http.StatusBadRequest)
        return
    }

    // Security: Validate and sanitize the "name" field.
    name, err := validateAndSanitizeName(payload.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Security: Respond with JSON using context-aware encoding.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    json.NewEncoder(w).Encode(map[string]string{"name": name})
}

// validateAndSanitizeName checks that the name is non-empty, reasonable length, and contains only safe characters.
// Security: Prevents injection, XSS, and other attacks via the "name" field.
func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("name must be between 1 and 100 characters")
    }
    // Allow only letters, numbers, spaces, hyphens, and underscores.
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
