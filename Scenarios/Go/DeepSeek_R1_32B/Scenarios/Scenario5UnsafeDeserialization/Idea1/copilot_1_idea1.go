package main

import (
	"encoding/json"
	"net/http"
	"gopkg.in/yaml.v3"
)

type Payload struct {
	Name string `yaml:"name"`
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	var payload Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, "Error parsing YAML: "+err.Error(), http.StatusBadRequest)
		return
	}

	if payload.Name == "" {
		http.Error(w, "Name field not found in payload", http.StatusBadRequest)
		return
	}

	response := map[string]string{
		"name": payload.Name,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, "Error encoding response: "+err.Error(), http.StatusInternalServerError)
	}
}
