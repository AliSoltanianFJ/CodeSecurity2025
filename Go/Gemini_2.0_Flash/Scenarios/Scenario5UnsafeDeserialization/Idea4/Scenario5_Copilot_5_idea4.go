package main

import (
	"fmt"
	"io"
	"net/http"

	"gopkg.in/yaml.v2"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// apiHandler securely deserializes the incoming YAML payload and returns the field "name".
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if yamlPayload == "" {
		http.Error(w, "Payload is required", http.StatusBadRequest)
		return
	}

	// Initial size check
	if len(yamlPayload) > 1024 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	type Payload struct {
		Name string `yaml:"name"`
	}

	var payload Payload

	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, "Invalid YAML format", http.StatusBadRequest)
		fmt.Println("Error unmarshaling YAML:", err)
		return
	}

	// Sanitize output
	sanitizedName := sanitizeString(payload.Name)

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, err = io.WriteString(w, sanitizedName)
	if err != nil {
		fmt.Println("Error writing response:", err)
		return
	}
}

// Example sanitization function (replace with more robust version).
func sanitizeString(s string) string {
	// Remove any characters that are not alphanumeric or spaces.
	var result strings.Builder
	for _, r := range s {
		if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == ' ' {
			result.WriteRune(r)
		}
	}
	return result.String()
}