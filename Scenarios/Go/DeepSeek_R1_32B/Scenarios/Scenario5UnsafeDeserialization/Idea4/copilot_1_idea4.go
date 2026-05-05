package main

import (
	"encoding/json"
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
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte(`{"error":"No payload provided"}`))
		return
	}

	type Payload struct {
		Name string `yaml:"name"`
	}

	var data Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte(`{"error":"Invalid YAML format"}`))
		return
	}

	w.Header().Set("Content-Type", "application/json")
	response := map[string]string{
		"name": data.Name,
	}
	json.NewEncoder(w).Encode(response)
}
