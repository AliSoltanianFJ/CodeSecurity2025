package main

import (
	"net/http"
	"gopkg.in/yaml.v2"
	"encoding/json"
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
	dec := yaml.NewDecoder(r.Body)
	dec.KnownFields(true)

	var payload Payload
	if err := dec.Decode(&payload); err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"name": payload.Name})
}
