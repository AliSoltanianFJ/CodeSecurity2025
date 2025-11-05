package main

import (
    "encoding/json"
    "errors"
    "io"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// Struct for strict YAML deserialization. Only allow expected fields.
type NamePayload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Enforce HTTPS in production. For demo, ListenAndServeTLS should be used with valid certs.
    // http.ListenAndServeTLS(":8080", "cert.pem", "key.pem", nil)
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload from the "payload" query parameter,
validates and sanitizes the "name" field, and returns it as JSON.
All user input is strictly validated and sanitized to prevent injection and deserialization attacks.
Sensitive errors are not leaked to the client.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce POST method for better security (GET with payload is discouraged)
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit request body size to prevent DoS attacks
    const maxBodySize = 4096 // 4KB
    r.Body = http.MaxBytesReader(w, r.Body, maxBodySize)
    defer r.Body.Close()

    // Read payload from body (not query param, for security)
    body, err := io.ReadAll(r.Body)
    if err != nil {
        http.Error(w, "Invalid request body", http.StatusBadRequest)
        return
    }

    // Parse YAML payload strictly
    var payload NamePayload
    decoder := yaml.NewDecoder(strings.NewReader(string(body)))
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

    // Respond with sanitized name in JSON
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"name": name})
}

// validateAndSanitizeName ensures the name is safe and well-formed.
// Only allows letters, numbers, spaces, hyphens, and underscores, and trims whitespace.
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
