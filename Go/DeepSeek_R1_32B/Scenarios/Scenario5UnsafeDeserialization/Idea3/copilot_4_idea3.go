package main

import (
	"encoding/json"
	"net/http"
	"regexp"
	"strings"
	"html"
	"gopkg.in/yaml.v2"
)

type Payload struct {
	Name string `yaml:"name" json:"name"`
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	yamlPayload := r.URL.Query().Get("payload")
	if len(yamlPayload) == 0 || len(yamlPayload) > 2048 {
		http.Error(w, "Invalid payload", http.StatusBadRequest)
		return
	}

	// Reject potentially dangerous content
	if strings.ContainsAny(yamlPayload, "<>{}()|`") {
		http.Error(w, "Invalid characters in payload", http.StatusBadRequest)
		return
	}

	var p Payload
	decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
	decoder.KnownFields(true)
	decoder.DisallowUnknownFields()
	decoder.IgnoreComments(true)
	if err := decoder.Decode(&p); err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Sanitize output to prevent XSS
	safeName := html.EscapeString(p.Name)

	// Validate name contains only allowed characters
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9\s]+$`, safeName)
	if !matched {
		http.Error(w, "Invalid name field", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"name": safeName})
}
