package main

import (
	"net/http"
	"encoding/json"
	"html"
	"errors"
	"gopkg.in/yaml.v2"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// NamePayload represents the expected YAML payload structure.
type NamePayload struct {
	Name string `yaml:"name"`
}

/*
apiHandler securely deserializes a YAML payload from the request body,
validates and sanitizes the 'name' field, and returns it in JSON format.

Security measures:
- Only accepts POST requests
- Limits request body size to 4KB
- Sanitizes all user input
- Validates the 'name' field according to strict security guidelines
- Does not leak internal errors to clients
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
	// Security: Only allow POST method
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Security: Limit request body size (4KB max)
	r.Body = http.MaxBytesReader(w, r.Body, 4096)
	defer r.Body.Close()

	// Parse YAML payload securely
	var payload NamePayload
	decoder := yaml.NewDecoder(r.Body)
	// Security: Only decode known fields and types
	decoder.KnownFields(true)
	if err := decoder.Decode(&payload); err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Security: Validate and sanitize 'name' field
	name, err := validateAndSanitizeName(payload.Name)
	if err != nil {
		http.Error(w, "Invalid name field", http.StatusBadRequest)
		return
	}

	// Return result in JSON format (no XSS risk due to sanitization)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"name": name})
}

/*
validateAndSanitizeName validates and sanitizes the 'name' field according to security guidelines.

Requirements:
- Length: 1-100 characters
- Only allowed characters: alphanumeric, underscore (_)
- No HTML special characters (sanitized via html.EscapeString)

Returns a sanitized string or an error if validation fails.
*/
func validateAndSanitizeName(name string) (string, error) {
	// Security: Sanitize input to prevent XSS
	sanitized := html.EscapeString(name)

	// Check length
	if len(sanitized) < 1 || len(sanitized) > 100 {
		return "", errors.New("name must be 1-100 characters")
	}

	// Validate allowed characters (alphanumeric and underscore only)
	for _, r := range sanitized {
		if !(r >= 'a' && r <= 'z') &&
			!(r >= 'A' && r <= 'Z') &&
			!(r >= '0' && r <= '9') &&
			r != '_' {
			return "", errors.New("name contains invalid characters")
		}
	}

	return sanitized, nil
}
