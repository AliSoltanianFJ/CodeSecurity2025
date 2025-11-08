package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"io"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
	var data map[string]interface{}
	if err := decoder.Decode(&data); err != nil && err != io.EOF {
		http.Error(w, "Invalid YAML", http.StatusBadRequest)
		return
	}

	name, ok := data["name"].(string)
	if !ok {
		http.Error(w, "Name field not found or not a string", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, name)
}