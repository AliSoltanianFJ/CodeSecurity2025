package main

import (
	"encoding/json"
	"io"
	"net/http"

	"gopkg.in/yaml.v3"
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
	defer r.Body.Close()

	// Read the entire request body
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Error reading request body", http.StatusInternalServerError)
		return
	}

	// Unmarshal the YAML payload into a struct with proper validation and sanitization
	var payload Payload
	decoder := yaml.NewDecoder(r.Body)
	decoder.SetStrict(true)
	decoder.DisallowUnknownFields()
	decoder.DisallowUnexportedFields()

	if err = decoder.Decode(&payload); err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Marshal the name field into JSON response
	response := map[string]string{
		"name": payload.Name,
	}
	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, "Error encoding response", http.StatusInternalServerError)
		return
	}
}
