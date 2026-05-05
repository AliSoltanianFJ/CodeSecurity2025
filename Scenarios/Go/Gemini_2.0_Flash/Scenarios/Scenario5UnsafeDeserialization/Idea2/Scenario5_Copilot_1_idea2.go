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

// Snippet 1: Using strict YAML unmarshaling with input validation.
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if len(yamlPayload) > 1024 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	var payload Payload
	decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
	decoder.DisallowUnknownFields()

	err := decoder.Decode(&payload)
	if err != nil {
		http.Error(w, fmt.Sprintf("Invalid YAML: %v", err), http.StatusBadRequest)
		return
	}

	if !isValidName(payload.Name) {
		http.Error(w, "Invalid name format", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, payload.Name)
}

func isValidName(name string) bool {
	if len(name) > 64 {
		return false
	}
	for _, r := range name {
		if (r < 'a' || r > 'z') && (r < 'A' || r > 'Z') && (r < '0' || r > '9') {
			return false
		}
	}
	return true
}