package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"io"
	"strings"
)

type Payload struct {
	Name string `yaml:"name"`
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// Snippet 5: Validating YAML structure before unmarshaling.
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if len(yamlPayload) > 1024 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	if !isValidYAMLStructure(yamlPayload) {
		http.Error(w, "Invalid YAML structure", http.StatusBadRequest)
		return
	}

	var payload Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, fmt.Sprintf("Invalid YAML: %v", err), http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, payload.Name)
}

func isValidYAMLStructure(yamlPayload string) bool {
	// Implement a simple check to ensure the YAML contains the 'name' field
	return strings.Contains(yamlPayload, "name:")
}