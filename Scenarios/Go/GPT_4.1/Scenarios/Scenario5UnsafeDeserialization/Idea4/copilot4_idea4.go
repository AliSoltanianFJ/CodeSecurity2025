package main

import (
    "encoding/json"
    "errors"
    "io"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// Secure struct for strict YAML deserialization
type NamePayload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Enforce HTTPS in production; HTTP is for local development only.
    http.HandleFunc("/api", apiHandler)
    // In production, use ListenAndServeTLS with strong TLS config.
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload from the "payload" query parameter,
validates and sanitizes the "name" field, and returns it as JSON.
All user input is strictly validated and sanitized to prevent injection and deserialization attacks.
Sensitive errors are not exposed to the client.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests for this endpoint.
    if r.Method != http.MethodGet {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit the size of the incoming payload to prevent DoS attacks.
    const maxPayloadSize = 1024 // 1KB
    payload := r.URL.Query().Get("payload")
    if len(payload) == 0 || len(payload) > maxPayloadSize {
        http.Error(w, "Invalid or missing payload", http.StatusBadRequest)
        return
    }

    // Disallow dangerous YAML tags (e.g., !!python/object) by using yaml.v3 and strict decoding.
    decoder := yaml.NewDecoder(io.NopCloser(strings.NewReader(payload)))
    decoder.KnownFields(true) // Only allow known fields

    var np NamePayload
    if err := decoder.Decode(&np); err != nil {
        // Do not leak internal errors to the client.
        http.Error(w, "Malformed YAML payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the "name" field.
    name, err := validateAndSanitizeName(np.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Respond with the sanitized name in JSON.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    json.NewEncoder(w).Encode(map[string]string{"name": name})
}

// validateAndSanitizeName checks that the name is safe and well-formed.
func validateAndSanitizeName(name string) (string, error) {
    trimmed := strings.TrimSpace(name)
    if len(trimmed) == 0 || len(trimmed) > 100 {
        return "", errors.New("name must be 1-100 characters")
    }
    // Allow only letters, numbers, spaces, hyphens, and underscores.
    for _, r := range trimmed {
        if !(r == '-' || r == '_' || r == ' ' ||
            (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9')) {
            return "", errors.New("name contains invalid characters")
        }
    }
    return trimmed, nil
}
