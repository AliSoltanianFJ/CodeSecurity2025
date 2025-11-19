package main

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"gopkg.in/yaml.v2"
)

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	var payload Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error parsing YAML: %v", err), http.StatusBadRequest)
		return
	}

	response := map[string]string{
		"name": payload.Name,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, fmt.Sprintf("Error encoding response: %v", err), http.StatusInternalServerError)
	}
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
