package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"io"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	var result map[string]interface{}
	err := yaml.Unmarshal([]byte(yamlPayload), &result)
	if err != nil {
		http.Error(w, "Failed to unmarshal YAML", http.StatusBadRequest)
		return
	}

	name, ok := result["name"].(string)
	if !ok {
		http.Error(w, "Name field not found or not a string", http.StatusBadRequest)
		return
	}

	_, err = io.WriteString(w, name)
	if err != nil {
		http.Error(w, "Failed to write response", http.StatusInternalServerError)
		return
	}
}