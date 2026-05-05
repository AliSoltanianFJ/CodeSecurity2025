package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"strings"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	lines := strings.Split(yamlPayload, "\n")
	nameValue := ""
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "name:") {
			parts := strings.SplitN(line, ":", 2)
			if len(parts) == 2 {
				nameValue = strings.TrimSpace(parts[1])
				break
			}
		}
	}

	if nameValue == "" {
		http.Error(w, "Name field not found", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, nameValue)
}