package main

import (
    "encoding/json"
    "errors"
    "fmt"
    "io"
    "net/http"
    "net/url"
    "strings"

    "gopkg.in/yaml.v3"
)

// Security: Only allow expected fields in the struct, and use strict unmarshalling.
// Avoids CWE-502 (Deserialization of Untrusted Data) by not using generic types or interfaces.
type Payload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Security: ListenAndServeTLS should be used in production to enforce HTTPS.
    // For demonstration, HTTP is used, but never use plain HTTP in production.
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload from the 'payload' query parameter,
validates and sanitizes the 'name' field, and returns it as JSON.
All errors are handled generically to avoid information leakage.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only allow GET requests for this endpoint.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit the size of the incoming payload to prevent DoS attacks.
    const maxPayloadSize = 2048 // bytes

    rawPayload := r.URL.Query().Get("payload")
    if rawPayload == "" {
        http.Error(w, "Missing payload", http.StatusBadRequest)
        return
    }

    // Security: URL-decode the payload safely.
    decodedPayload, err := url.QueryUnescape(rawPayload)
    if err != nil {
        http.Error(w, "Invalid payload encoding", http.StatusBadRequest)
        return
    }

    // Security: Limit the size of the decoded payload.
    if len(decodedPayload) > maxPayloadSize {
        http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Security: Use yaml.Decoder with KnownFields to reject unknown fields.
    var payload Payload
    decoder := yaml.NewDecoder(strings.NewReader(decodedPayload))
    decoder.KnownFields(true)
    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Security: Validate and sanitize the 'name' field.
    name, err := validateAndSanitizeName(payload.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Security: Set secure headers.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Cache-Control", "no-store")

    // Return the sanitized name as JSON.
    resp := map[string]string{"name": name}
    json.NewEncoder(w).Encode(resp)
}

// validateAndSanitizeName checks that the name is safe and valid.
// Security: Prevents XSS, injection, and other attacks by restricting allowed characters and length.
func validateAndSanitizeName(name string) (string, error) {
    const maxNameLen = 64
    if len(name) == 0 || len(name) > maxNameLen {
        return "", errors.New("name must be non-empty and at most 64 characters")
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
