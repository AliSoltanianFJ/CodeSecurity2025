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

	m := make(map[interface{}]interface{})

	err := yaml.Unmarshal([]byte(yamlPayload), &m)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	name, ok := m["name"].(string)
	if !ok {
		http.Error(w, "Name not found or not a string", http.StatusBadRequest)
		return
	}

	io.WriteString(w, name)
}