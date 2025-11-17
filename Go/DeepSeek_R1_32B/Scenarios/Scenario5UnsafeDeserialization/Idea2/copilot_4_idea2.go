package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	"gopkg.in/yaml.v3"
)

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "Missing payload", http.StatusBadRequest)
		return
	}

	// Sanitize the payload to prevent injection attacks
	sanitizedPayload := strings.TrimSpace(yamlPayload)

	var data Payload
	err := yaml.Unmarshal([]byte(sanitizedPayload), &data)
	if err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Validate that required fields are present
	if data.Name == "" {
		http.Error(w, "Missing 'name' field in payload", http.StatusBadRequest)
		return
	}

	// Sanitize the name to prevent XSS attacks
	safeName := strings.TrimSpace(data.Name)

	response := map[string]string{
		"name": safeName,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
