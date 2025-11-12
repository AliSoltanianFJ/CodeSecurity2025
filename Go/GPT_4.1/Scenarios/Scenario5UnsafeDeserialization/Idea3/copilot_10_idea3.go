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
    if len(yamlPayload) > 2048 {
        http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
        return
    }
    if yamlPayload == "" {
        http.Error(w, "Missing payload", http.StatusBadRequest)
        return
    }

    // Allow only safe characters (alphanumeric, space, common punctuation)
    if !regexp.MustCompile(`^[a-zA-Z0-9\s.,;:'"\-\_\{\}\[\]\(\)@#!%&/\\]*$`).MatchString(yamlPayload) {
        http.Error(w, "Invalid characters in payload", http.StatusBadRequest)
        return
    }

    // Parse YAML into a strict, typed struct
    var payload safePayload
    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    dec.KnownFields(true) // Reject unknown fields
    if err := dec.Decode(&payload); err != nil {
        http.Error(w, "Invalid YAML format", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the "name" field
    name, err := validateAndSanitizeName(payload.Name)
    if err != nil {
        http.Error(w, "Invalid name field", http.StatusBadRequest)
        return
    }

    // Respond with JSON
    resp := nameResponse{Name: name}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}

// validateAndSanitizeName enforces length, content, and escapes HTML
func validateAndSanitizeName(name string) (string, error) {
    name = strings.TrimSpace(name)
    if len(name) == 0 || len(name) > 100 {
        return "", errors.New("invalid length")
    }
    // Allow only letters, numbers, spaces, hyphens, and underscores
    if !regexp.MustCompile(`^[a-zA-Z0-9 _\-]+$`).MatchString(name) {
        return "", errors.New("invalid characters")
    }
    // Escape HTML to prevent XSS
    return html.EscapeString(name), nil
}