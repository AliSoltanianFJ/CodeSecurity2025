package main

import (
	"encoding/json"
	"net/http"
	"gopkg.in/yaml.v2"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

type Payload struct {
	Name string `yaml:"name"`
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
		http.Error(w, "Invalid YAML payload or missing 'name' field", http.StatusBadRequest)
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
	json.NewEncoder(w).Encode(response)
}
