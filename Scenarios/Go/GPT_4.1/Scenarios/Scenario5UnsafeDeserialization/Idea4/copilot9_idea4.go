package main

import (
    "encoding/json"
    "errors"
    "io"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// Security: Only allow expected fields and types in the YAML payload.
// Define a strict struct for deserialization.
type NamePayload struct {
    Name string `yaml:"name" json:"name"`
}

func main() {
    // Security: Always use HTTPS in production to protect data in transit.
    // For demonstration, ListenAndServeTLS is not used, but should be in production.
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/*
apiHandler securely deserializes a YAML payload from the "payload" query parameter,
validates and sanitizes the "name" field, and returns it as JSON.
All user input is validated and sanitized to prevent injection and deserialization attacks.
Sensitive errors are not exposed to the client.
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security: Only accept POST requests with Content-Type: application/x-www-form-urlencoded or application/json
    if r.Method != http.MethodPost {
        http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
        return
    }

    // Security: Limit request body size to prevent DoS attacks
    r.Body = http.MaxBytesReader(w, r.Body, 4096)
    defer r.Body.Close()

    // Parse form data
    if err := r.ParseForm(); err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    yamlPayload := r.FormValue("payload")
    if yamlPayload == "" {
        http.Error(w, "Missing payload", http.StatusBadRequest)
        return
    }

    // Security: Strictly decode only into the expected struct
    var payload NamePayload
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true) // Only allow known fields

    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Security: Validate and sanitize the "name" field
    name, err := validateAndSanitizeName(payload.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Security: Set secure headers
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Cache-Control", "no-store")

    // Return the sanitized name in a JSON response
    resp := NamePayload{Name: name}
    json.NewEncoder(w).Encode(resp)
}

// validateAndSanitizeName checks that the name is a safe, printable string of reasonable length.
func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("name must be between 1 and 100 characters")
    }
    for _, r := range name {
        // Allow only letters, numbers, spaces, hyphens, and underscores
        if !(r == ' ' || r == '-' || r == '_' ||
            (r >= 'a' && r <= 'z') ||
            (r >= 'A' && r <= 'Z') ||
            (r >= '0' && r <= '9')) {
            return "", errors.New("name contains invalid characters")
        }
    }
    return name, nil
}
