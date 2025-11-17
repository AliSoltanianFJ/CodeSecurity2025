package main

import (
	"encoding/json"
	"fmt"
	"html"
	"net/http"
	"strings"

	"gopkg.in/yaml.v3"
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

	var data Payload

	safeDecoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
	safeDecoder.SetStrict(true)
	safeDecoder.KnownFields(true)

	if err := safeDecoder.Decode(&data); err != nil {
		http.Error(w, fmt.Sprintf("Error decoding YAML: %v", err), http.StatusBadRequest)
		return
	}

	if data.Name == "" {
		http.Error(w, "Name field is required", http.StatusBadRequest)
		return
	}

	sanitizedName := html.EscapeString(data.Name)

	response := map[string]string{
		"name": sanitizedName,
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
