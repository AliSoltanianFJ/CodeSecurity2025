package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"regexp"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	re := regexp.MustCompile(`name:\s*([a-zA-Z0-9]+)`)
	match := re.FindStringSubmatch(yamlPayload)

	if len(match) < 2 {
		http.Error(w, "Name not found", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, match[1])
}