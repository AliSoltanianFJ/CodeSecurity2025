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

	type Payload struct {
		Name interface{} `yaml:"name"`
	}

	var payload Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	name, ok := payload.Name.(string)
	if !ok {
		http.Error(w, "Name not found or is not a string", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, name)
}