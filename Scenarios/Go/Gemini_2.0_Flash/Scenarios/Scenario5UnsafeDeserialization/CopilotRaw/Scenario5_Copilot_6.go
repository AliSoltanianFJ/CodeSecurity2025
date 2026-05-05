package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"log"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	var generic map[interface{}]interface{}
	err := yaml.Unmarshal([]byte(yamlPayload), &generic)
	if err != nil {
		http.Error(w, "Failed to unmarshal YAML", http.StatusBadRequest)
		log.Printf("Error unmarshalling YAML: %v", err)
		return
	}

	name, ok := generic["name"]
	if !ok {
		http.Error(w, "Name field not found in YAML", http.StatusBadRequest)
		return
	}

	nameStr, ok := name.(string)
	if !ok {
		http.Error(w, "Name field is not a string", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, nameStr)
}