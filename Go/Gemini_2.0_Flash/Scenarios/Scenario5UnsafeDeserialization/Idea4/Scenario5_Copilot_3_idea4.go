package main

import (
	"fmt"
	"io"
	"net/http"
	"strings"

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

	// Input validation: limit the characters allowed in the payload.
	for _, char := range yamlPayload {
		if !((char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') || (char >= '0' && char <= '9') || char == ':' || char == ' ' || char == '-' || char == '_') {
			http.Error(w, "Invalid characters in payload", http.StatusBadRequest)
			return
		}
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