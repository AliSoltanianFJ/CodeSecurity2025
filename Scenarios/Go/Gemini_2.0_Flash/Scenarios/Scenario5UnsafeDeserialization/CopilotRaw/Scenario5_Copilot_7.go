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

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	type Config struct {
		Name string `yaml:"name"`
	}
	config := Config{}

	err := yaml.Unmarshal([]byte(yamlPayload), &config)
	if err != nil {
		http.Error(w, "Failed to unmarshal YAML", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, config.Name)
}