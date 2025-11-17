package main

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"
	"strings"

	"gopkg.in/yaml.v3"
)

type Payload struct {
	Name string `yaml:"name"`
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	payload := r.URL.Query().Get("payload")
	if payload == "" {
		http.Error(w, "Payload is required", http.StatusBadRequest)
		return
	}

	// Limit maximum allowed size for payload to prevent memory exhaustion attacks
	maxSize := 1024 // bytes
	if len(payload) > maxSize {
		http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
		return
	}

	// Initialize decoder with strict mode and without allowing recursive structures
	decoder := yaml.NewDecoder(strings.NewReader(payload))
	decoder.SetStrict(true)
	decoder.SetRaw(false)

	var data Payload
	if err := decoder.Decode(&data); err != nil {
		log.Printf("YAML decoding error: %v", err)
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	// Validate name field to prevent unexpected values
	if data.Name == "" || len(data.Name) > 100 {
		http.Error(w, "Invalid name format", http.StatusBadRequest)
		return
	}

	// Return the name in JSON format
	response := map[string]string{
		"name": data.Name,
	}
	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		log.Printf("Error encoding response: %v", err)
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
}
