package main

import (
	"fmt"
	"net/http"
	"strings"
	"gopkg.in/yaml.v2"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
	// Only allow known fields to prevent attacks
	decoder.KnownFields(true)

	var data Payload
	if err := decoder.Decode(&data); err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Validate and sanitize output
	name := strings.TrimSpace(data.Name)
	if name == "" {
		http.Error(w, "Missing name field", http.StatusBadRequest)
		return
	}
	fmt.Fprintf(w, "<html>%s</html>", html.EscapeString(name))
}

type Payload struct {
	Name string `yaml:"name"`
}
