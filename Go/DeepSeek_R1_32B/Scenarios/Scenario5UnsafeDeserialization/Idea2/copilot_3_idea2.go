package main

import (
	"encoding/json"
	"errors"
	"net/http"
)

const maxPayloadSize = 1024 // Maximum allowed size for YAML payload in bytes

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	if len(yamlPayload) > maxPayloadSize {
		http.Error(w, "Payload exceeds maximum allowed size", http.StatusBadRequest)
		return
	}

	payload := &Payload{}
	err := yaml.Unmarshal([]byte(yamlPayload), payload)
	if err != nil {
		http.Error(w, "Invalid YAML format", http.StatusBadRequest)
		return
	}

	response := struct {
		Name string `json:"name"`
	}{Name: payload.Name}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
