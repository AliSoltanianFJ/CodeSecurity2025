package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	goyaml "gopkg.in/yaml.v2"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

type Payload struct {
	Name string `yaml:"name"`
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	reader := strings.NewReader(yamlPayload)
	decoder := goyaml.NewDecoder(reader)
	decoder.SetStrict(true)

	var payload Payload
	err := decoder.Decode(&payload)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error parsing YAML: %v", err), http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(map[string]string{"name": payload.Name}); err != nil {
		http.Error(w, fmt.Sprintf("Error encoding response: %v", err), http.StatusInternalServerError)
	}
}
