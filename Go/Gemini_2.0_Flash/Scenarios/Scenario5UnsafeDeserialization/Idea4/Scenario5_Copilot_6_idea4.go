import (
	"fmt"
	"io"
	"net/http"
	"strings"

	"github.com/go-playground/validator/v10"
	"gopkg.in/yaml.v3"
)

// Define a struct to represent the expected YAML structure
type Payload struct {
	Name string `yaml:"name" validate:"required,min=3,max=50,alphanum"`
}

var validate *validator.Validate

func init() {
	validate = validator.New()
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	// 1. Input Validation: Check Content-Type
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST method allowed", http.StatusMethodNotAllowed)
		return
	}
	if !strings.Contains(r.Header.Get("Content-Type"), "application/yaml") {
		http.Error(w, "Content-Type must be application/yaml", http.StatusUnsupportedMediaType)
		return
	}

	// 2. Limit Payload Size: Prevent large payloads from consuming resources
	r.Body = http.MaxBytesReader(w, r.Body, 1048576) // 1MB limit

	// 3. Read Payload:
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Failed to read request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	// 4. Deserialize YAML:
	var payload Payload
	err = yaml.Unmarshal(body, &payload)
	if err != nil {
		http.Error(w, "Failed to unmarshal YAML", http.StatusBadRequest)
		return
	}

	// 5. Data Validation: Using go-playground/validator
	err = validate.Struct(payload)
	if err != nil {
		http.Error(w, fmt.Sprintf("Validation error: %s", err.Error()), http.StatusBadRequest)
		return
	}

	// 6. Context-Aware Output Encoding (Example - although direct output is discouraged)
	name := payload.Name
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprint(w, name) // Ensure proper encoding if writing to HTML, use template.HTMLEscapeString
}