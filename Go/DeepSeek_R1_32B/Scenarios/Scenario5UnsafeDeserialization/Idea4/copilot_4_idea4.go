package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"net/http"
	"gopkg.in/yaml.v3"
)

var ErrInvalidPayload = errors.New("invalid payload")

func apiHandler(w http.ResponseWriter, r *http.Request) {
	payload := r.URL.Query().Get("payload")
	if len(payload) == 0 {
		http.Error(w, "no payload provided", http.StatusBadRequest)
		return
	}

	// Maximum allowed size for the payload to prevent memory exhaustion attacks
	maxPayloadSize := 1024 * 1024 // 1MB
	if len(payload) > maxPayloadSize {
		http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
		return
	}

	type Payload struct {
		Name string `yaml:"name"`
	}

	data := &Payload{}

	// Use StrictMode to prevent execution of arbitrary code during deserialization
	decoder := yaml.NewDecoder(bytes.NewReader([]byte(payload)))
	decoder.SetStrict(true)

	if err := decoder.Decode(data); err != nil {
		http.Error(w, "invalid YAML payload", http.StatusBadRequest)
		return
	}

	if data.Name == "" {
		http.Error(w, "name field is required", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(map[string]string{"name": data.Name}); err != nil {
		// This should not happen as we've already validated the response structure
		http.Error(w, "internal server error", http.StatusInternalServerError)
	}
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
