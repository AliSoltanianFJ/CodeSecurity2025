package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
)

type MyData struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "Payload is required", http.StatusBadRequest)
		return
	}

	data := MyData{}
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, "Invalid YAML", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, data.Name)
}