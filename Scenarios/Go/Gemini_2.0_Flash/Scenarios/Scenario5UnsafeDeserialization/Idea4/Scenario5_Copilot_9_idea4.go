import (
	"fmt"
	"io"
	"log"
	"net/http"
	"regexp"
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

	contentType := r.Header.Get("Content-Type")
	if !strings.Contains(contentType, "application/yaml") {
		http.Error(w, "Content-Type must be application/yaml", http.StatusUnsupportedMediaType)
		return
	}

	body, err := io.ReadAll(io.LimitReader(r.Body, 4096)) // Limit to 4KB
	if err != nil {
		log.Printf("Error reading body: %v", err) // Log the error
		http.Error(w, "Failed to read request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	var payload Payload
	err = yaml.Unmarshal(body, &payload)
	if err != nil {
		log.Printf("Error unmarshaling YAML: %v", err) // Log the error
		http.Error(w, "Failed to unmarshal YAML", http.StatusBadRequest)
		return
	}

	// Use regular expression for validation
	regex := regexp.MustCompile(`^[a-zA-Z0-9]{3,20}$`)
	if !regex.MatchString(payload.Name) {
		http.Error(w, "Invalid name format", http.StatusBadRequest)
		return
	}

	// Context-aware escaping (example: HTML escaping)
	escapedName := fmt.Sprintf("%s", payload.Name) // Replace with proper escaping for the target context.
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprint(w, escapedName)
}