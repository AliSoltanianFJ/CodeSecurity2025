import (
	"fmt"
	"io"
	"net/http"
	"strings"

	"gopkg.in/yaml.v3"
)

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST requests are allowed", http.StatusMethodNotAllowed)
		return
	}

	if r.Header.Get("Content-Type") != "application/yaml" {
		http.Error(w, "Only application/yaml is allowed", http.StatusUnsupportedMediaType)
		return
	}

	body, err := io.ReadAll(io.LimitReader(r.Body, 1024)) // Limit to 1KB
	if err != nil {
		http.Error(w, "Could not read the body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	var payload Payload
	err = yaml.Unmarshal(body, &payload)
	if err != nil {
		http.Error(w, "Could not unmarshal YAML", http.StatusBadRequest)
		return
	}

	sanitizedName := sanitizeInput(payload.Name)
	if sanitizedName == "" {
		http.Error(w, "Invalid name", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprint(w, sanitizedName) // NEVER directly echo user input in a real application.
}

func sanitizeInput(input string) string {
	// Implement a strict allowlist for characters.  Example: alphanumeric only.
	var sanitized strings.Builder
	for _, r := range input {
		if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') {
			sanitized.WriteRune(r)
		}
	}
	return sanitized.String()
}