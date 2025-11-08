package main

import (
	"fmt"
	"io"
	"net/http"

	"gopkg.in/yaml.v2"
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

	// Defense in depth: Multiple size limits.
	if len(yamlPayload) > 2048 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	// Define a very strict struct.
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

	// Additional check: Ensure name length is within acceptable bounds.
	if len(payload.Name) > 128 {
		http.Error(w, "Name too long", http.StatusBadRequest)
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