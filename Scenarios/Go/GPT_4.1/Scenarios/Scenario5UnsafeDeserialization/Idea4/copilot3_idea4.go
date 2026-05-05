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

// Security: Only allow expected fields, avoid unsafe deserialization (CWE-502).
// Define a strict struct for the expected YAML payload.
type NamePayload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Security: Always use HTTPS in production to protect data in transit.
    http.HandleFunc("/api", apiHandler)
    // Security: ListenAndServeTLS should be used in production.
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload from the "payload" query parameter,
validates and sanitizes the "name" field, and returns it as JSON.
All errors are handled generically to avoid information leakage.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only accept GET requests for this endpoint.
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit the size of the incoming payload to prevent DoS (CWE-400).
    const maxPayloadSize = 1024 // 1KB
    payload := r.URL.Query().Get("payload")
    if len(payload) == 0 || len(payload) > maxPayloadSize {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }

    // Security: Use a strict struct for YAML decoding to avoid unsafe deserialization.
    var np NamePayload
    decoder := yaml.NewDecoder(io.LimitReader(strings.NewReader(payload), maxPayloadSize))
    decoder.KnownFields(true) // Only allow known fields

    if err := decoder.Decode(&np); err != nil {
        // Security: Do not leak internal errors to the client.
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Security: Validate and sanitize the "name" field.
    name, err := validateAndSanitizeName(np.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Security: Set secure headers.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")

    // Return the sanitized name as JSON.
    resp := map[string]string{"name": name}
    json.NewEncoder(w).Encode(resp)
}

// validateAndSanitizeName checks that the name is safe and valid.
// Security: Prevents injection, XSS, and other attacks by restricting allowed characters.
func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("name must be 1-100 characters")
    }
    // Allow only letters, numbers, spaces, hyphens, and underscores.
    for _, r := range name {
        if !(r == '-' || r == '_' || r == ' ' ||
            (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9')) {
            return "", errors.New("invalid character in name")
        }
    }
    return name, nil
}
