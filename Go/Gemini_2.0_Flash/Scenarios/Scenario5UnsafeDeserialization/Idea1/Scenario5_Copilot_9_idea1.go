package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	var result map[string]interface{}
	err := yaml.Unmarshal([]byte(yamlPayload), &result)
	if err != nil {
		http.Error(w, "Invalid YAML", http.StatusBadRequest)
		return
	}

	name, ok := result["name"]
	if !ok {
		http.Error(w, "Name field not found", http.StatusBadRequest)
		return
	}

	nameStr, ok := name.(string)
	if !ok {
		http.Error(w, "Name is not a string", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, nameStr)
}