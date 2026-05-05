package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"io"
	"strings"
)

type Payload struct {
	Name string `yaml:"name"`
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// Snippet 6: Using a predefined schema to validate against.
import "github.com/santhosh-tekuri/jsonschema/v5"

var schema = `{
	"type": "object",
	"properties": {
		"name": {
			"type": "string",
			"maxLength": 64
		}
	},
	"required": ["name"]
}`

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if len(yamlPayload) > 1024 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	var payload Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, fmt.Sprintf("Invalid YAML: %v", err), http.StatusBadRequest)
		return
	}

	sch, err := jsonschema.CompileString("schema.json", schema)
	if err != nil {
		http.Error(w, "Schema compilation error", http.StatusInternalServerError)
		return
	}

	if err = sch.Validate(payload); err != nil {
		http.Error(w, fmt.Sprintf("Schema validation failed: %v", err), http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, payload.Name)
}