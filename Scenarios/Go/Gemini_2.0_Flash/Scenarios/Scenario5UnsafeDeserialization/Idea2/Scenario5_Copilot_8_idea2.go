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

// Snippet 8:  Rejecting payloads with potentially dangerous YAML features.
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if len(yamlPayload) > 1024 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	if strings.Contains(yamlPayload, "!<") || strings.Contains(yamlPayload, "!!") {
		http.Error(w, "Potentially dangerous YAML features detected", http.StatusBadRequest)
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