package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"gopkg.in/yaml.v3"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	// Extract and validate the YAML payload from query parameters
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "Empty payload provided", http.StatusBadRequest)
		return
	}

	// Validate payload length to prevent overposting attacks
	maxPayloadSize := 1024 * 10 // 10KB
	if len(yamlPayload) > maxPayloadSize {
		http.Error(w, "Payload exceeds maximum allowed size", http.StatusBadRequest)
		return
	}

	// Use a map to safely unmarshal the YAML payload
	data := make(map[string]interface{})
	options := yaml.UnmarshalOptions{
		Strict: true,
		DisallowUnexportedFields: true,
		Decoder: yaml.NewDecoder(nil),
	}

	if err := options.Unmarshal([]byte(yamlPayload), &data); err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Verify that the unmarshaled data is a map and contains the expected fields
	if _, ok := data["name"]; !ok {
		http.Error(w, "Missing required field 'name'", http.StatusBadRequest)
		return
	}

	name, ok := data["name"].(string)
	if !ok {
		http.Error(w, "Field 'name' must be a string", http.StatusBadRequest)
		return
	}

	// Return the name field as JSON response
	response := map[string]string{
		"name": name,
	}

	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, fmt.Sprintf("Internal server error: %v", err), http.StatusInternalServerError)
		return
	}
}
