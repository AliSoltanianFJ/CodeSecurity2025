package main

import (
	"errors"
	"html"
	"io/ioutil"
	"net/http"
	"regexp"

	gopkg "gopkg.in/yaml.v2"
)

// NamePayload represents the expected YAML payload structure.
type NamePayload struct {
	Name string `yaml:"name" json:"name"`
}

func main() {
	// Only allow expected HTTP methods and enforce request size limits.
	http.HandleFunc("/api", handleAPIRequest)
	http.ListenAndServe(":8080", nil)
}

/*
handleAPIRequest securely deserializes a YAML payload from the request body,
validates and sanitizes the "name" field, and returns it with appropriate security headers.
All error handling is done with generic responses to prevent information leakage.
*/
func handleAPIRequest(w http.ResponseWriter, r *http.Request) {
	// Only allow POST requests.
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Enforce request size limit (10KB max).
	r.Body = http.MaxBytesReader(w, r.Body, 1024*10)

	// Parse YAML payload safely.
	var payload NamePayload
	decoder := gopkg.NewDecoder(r.Body)
	decoder.KnownFields(true) // Only allow known fields.

	if err := decoder.Decode(&payload); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Validate and sanitize the name field.
	name, err := validateAndSanitizeName(payload.Name)
	if err != nil {
		http.Error(w, "Invalid name field", http.StatusBadRequest)
		return
	}

	// Set secure headers.
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")

	// Return the sanitized name securely.
	w.Write([]byte(name))
}

/*
validateAndSanitizeName validates that the provided name is safe according to security guidelines:
- Enforces length limits (1-100 characters)
- Only allows safe characters (letters, numbers, spaces, hyphens, underscores)
- Sanitizes for HTML output to prevent XSS
*/
func validateAndSanitizeName(name string) (string, error) {
	name = strings.TrimSpace(name)
	if len(name) == 0 || len(name) > 100 {
		return "", errors.New("name length invalid")
	}

	// Allow only safe characters and sanitize HTML.
	allowedChars := regexp.MustCompile(`^[a-zA-Z0-9 \-_]+$`)
	if !allowedChars.MatchString(name) {
		return "", errors.New("invalid characters in name")
	}

	// Escape HTML to prevent XSS.
	safeName := html.EscapeString(name)
	return safeName, nil
}
