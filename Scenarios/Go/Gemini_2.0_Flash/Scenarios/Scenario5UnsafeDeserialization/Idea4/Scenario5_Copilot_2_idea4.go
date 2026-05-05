package main

import (
	"fmt"
	"io"
	"net/http"
	"strings"

	"gopkg.in/yaml.v2" // Using a popular YAML library
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// apiHandler securely deserializes the incoming YAML payload and returns the field "name".
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if yamlPayload == "" {
		http.Error(w, "Payload is required", http.StatusBadRequest)
		return
	}

	// Input validation: Check for potentially malicious content using a blacklist approach.
	if strings.Contains(yamlPayload, "!!") || strings.Contains(yamlPayload, "`") {
		http.Error(w, "Invalid payload content", http.StatusBadRequest)
		return
	}

	type Payload struct {
		Name string `yaml:"name"`
	}

	var payload Payload

	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, "Invalid YAML format", http.StatusBadRequest)
		fmt.Println("Error unmarshaling YAML:", err)
		return
	}

	name := payload.Name

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, err = io.WriteString(w, name)
	if err != nil {
		fmt.Println("Error writing response:", err)
		return
	}
}