package main

import (
	"net/http"
	"gopkg.in/yaml.v3"
)

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
	
	var payload struct {
		Name string `yaml:"name"`
	}
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, "Error parsing YAML or missing 'name' field", http.StatusBadRequest)
		return
	}
	if payload.Name == "" {
		http.Error(w, "Name field not found in payload", http.StatusBadRequest)
		return
	}
	
	w.Header().Set("Content-Type", "text/plain")
	w.Write([]byte(payload.Name))
}
