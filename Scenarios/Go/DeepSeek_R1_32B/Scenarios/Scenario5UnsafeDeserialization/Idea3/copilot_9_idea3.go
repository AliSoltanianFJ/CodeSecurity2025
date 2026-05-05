package main

import (
	"net/http"
	"strings"
	"gopkg.in/yaml.v2"
	"encoding/json"
	"errors"
)

type Payload struct {
	Name string `yaml:"name"`
}

var (
	maxPayloadSize = 4096 // 4KB maximum payload size
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if len(yamlPayload) == 0 {
		http.Error(w, "Missing payload parameter", http.StatusBadRequest)
		return
	}

	// Security: Limit payload size to prevent DoS attacks
	if len(yamlPayload) > maxPayloadSize {
		http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
		return
	}

	var p Payload
	decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
	// Security: Only allow known fields to prevent injection attacks
	decoder.KnownFields(true)
	if err := decoder.Decode(&p); err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Validate and sanitize the name field
	name, err := validateAndSanitizeName(p.Name)
	if err != nil {
		http.Error(w, "Invalid name field", http.StatusBadRequest)
		return
	}

	// Security: Do not leak internal errors to clients
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"name": name})
}

// validateAndSanitizeName validates and sanitizes the name field.
// Returns an error if validation fails.
func validateAndSanitizeName(name string) (string, error) {
	// Security: Enforce reasonable length to prevent buffer overflows
	if len(name) == 0 || len(name) > 256 {
		return "", errors.New("invalid name length")
	}

	// Security: Only allow printable ASCII characters
	for _, r := range name {
		if !isPrintableASCII(r) {
			return "", errors.New("invalid character in name")
		}
	}

	// Trim whitespace from the name (not strictly necessary, but clean)
	name = strings.TrimSpace(name)

	return name, nil
}

// isPrintableASCII checks if a rune is a printable ASCII character.
func isPrintableASCII(r rune) bool {
	return r >= 32 && r <= 126
}
