package main

import (
	"github.com/go-yaml/yaml"
	"net/http"
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

	type Payload struct {
		Name string `yaml:"name"`
	}

	var data Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, "Error parsing YAML or missing 'name' field", http.StatusBadRequest)
		return
	}

	w.WriteHeader(http.StatusOK)
	w.Write([]byte(data.Name))
}
