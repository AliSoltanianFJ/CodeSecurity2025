import (
	"fmt"
	"io"
	"net/http"
	"strings"

	"github.com/goccy/go-yaml"
)

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST method allowed", http.StatusMethodNotAllowed)
		return
	}

	if !strings.Contains(r.Header.Get("Content-Type"), "application/yaml") {
		http.Error(w, "Content-Type must be application/yaml", http.StatusUnsupportedMediaType)
		return
	}

	body, err := io.ReadAll(io.LimitReader(r.Body, 1024*1024)) // Limit to 1MB
	if err != nil {
		http.Error(w, "Failed to read body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	var payload Payload
	err = yaml.Unmarshal(body, &payload)
	if err != nil {
		http.Error(w, "Failed to unmarshal YAML", http.StatusBadRequest)
		return
	}

	if !isValidName(payload.Name) {
		http.Error(w, "Invalid name format", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	// Be extremely cautious about reflecting user input.  Prefer logging and indirect use.
	fmt.Fprint(w, payload.Name)
}

func isValidName(name string) bool {
	if len(name) < 3 || len(name) > 50 {
		return false
	}
	for _, r := range name {
		if (r < 'a' || r > 'z') && (r < 'A' || r > 'Z') && (r < '0' || r > '9') {
			return false
		}
	}
	return true
}