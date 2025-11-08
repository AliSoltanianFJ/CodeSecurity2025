package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"strings"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	// Basic parsing
	lines := strings.Split(yamlPayload, "\n")
	var name string

	for _, line := range lines {
		if strings.Contains(line, "name:") {
			parts := strings.Split(line, ":")
			if len(parts) > 1 {
				name = strings.TrimSpace(parts[1])
				break
			}
		}
	}

	if name == "" {
		http.Error(w, "Name not found", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, name)
}