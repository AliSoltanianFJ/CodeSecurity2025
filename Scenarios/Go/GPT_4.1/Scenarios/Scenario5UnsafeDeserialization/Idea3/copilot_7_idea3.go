package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    // Define a strict struct for only expected fields
    type Payload struct {
        Name string `yaml:"name" json:"name"`
    }

    // Reject empty payloads early
    if yamlPayload == "" {
        http.Error(w, "Missing payload parameter", http.StatusBadRequest)
        return
    }

    // Limit payload size to prevent DoS attacks
    const maxPayloadSize = 2048
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Decode YAML strictly to prevent unknown fields (security best practice)
    var payload Payload
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true)
    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Validate the "name" field (e.g., length, characters)
    if err := validateName(payload.Name); err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Escape output to prevent XSS
    resp := Payload{
        Name: html.EscapeString(payload.Name),
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// Strict validation for the "name" field
func validateName(name string) error {
    if name == "" {
        return errors.New("name is required")
    }
    if len(name) > 100 {
        return errors.New("name too long")
    }
    // Allow only letters, numbers, spaces, hyphens, and underscores
    validName := regexp.MustCompile(`^[a-zA-Z0-9 _\-]+$`)
    if !validName.MatchString(name) {
        return errors.New("name contains invalid characters")
    }
    return nil
}