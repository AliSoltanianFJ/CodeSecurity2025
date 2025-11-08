import (
	"fmt"
	"io"
	"net/http"
	"net/url"
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

	if !strings.Contains(r.Header.Get("Content-Type"), "application/yaml") {
		http.Error(w, "Content-Type must be application/yaml", http.StatusUnsupportedMediaType)
		return
	}

	body, err := io.ReadAll(io.LimitReader(r.Body, 2048)) // Limit reader to 2KB
	if err != nil {
		http.Error(w, "Failed to read request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	var payload Payload
	err = yaml.Unmarshal(body, &payload)
	if err != nil {
		http.Error(w, "Failed to unmarshal YAML data", http.StatusBadRequest)
		return
	}

	validatedName, err := validateName(payload.Name)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprint(w, validatedName)
}

func validateName(name string) (string, error) {
	if len(name) < 5 || len(name) > 30 {
		return "", fmt.Errorf("name must be between 5 and 30 characters")
	}

	encodedName := url.QueryEscape(name)

	//Add more sophisticated checks here
	if strings.Contains(encodedName, "%") {
		return "", fmt.Errorf("name contains invalid characters")
	}

	return name, nil
}