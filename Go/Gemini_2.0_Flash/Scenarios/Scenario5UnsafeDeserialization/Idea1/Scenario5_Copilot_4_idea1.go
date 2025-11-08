package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
)

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "Payload is required", http.StatusBadRequest)
		return
	}

	var payload Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, "Invalid YAML format", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, payload.Name)
}