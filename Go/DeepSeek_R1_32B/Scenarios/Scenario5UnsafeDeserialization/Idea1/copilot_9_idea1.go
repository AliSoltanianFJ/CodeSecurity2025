package main

import (
	"fmt"
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

	var data Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, "Error parsing YAML or missing 'name' field", http.StatusBadRequest)
		return
	}

	if data.Name == "" {
		http.Error(w, "'name' field is empty in payload", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	fmt.Fprintf(w, "%s", data.Name)
}
