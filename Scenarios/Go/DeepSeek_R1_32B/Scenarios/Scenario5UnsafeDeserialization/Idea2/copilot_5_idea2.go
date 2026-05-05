package main

import (
	"encoding/base64"
	"encoding/json"
	"encoding/yaml"
	"fmt"
	"html"
	"net/http"
	"strings"

	"gopkg.in/yaml.v3"
)

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	payload := r.URL.Query().Get("payload")
	if payload == "" {
		http.Error(w, "Missing payload", http.StatusBadRequest)
		return
	}

	maxPayloadSize := 1024 * 1024 // 1MB
	if len(payload) > maxPayloadSize {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	// Base64 decode the payload to prevent issues with URL encoding
	payloadBytes, err := base64.URLEncoding.DecodeString(payload)
	if err != nil {
		http.Error(w, "Invalid payload encoding", http.StatusBadRequest)
		return
	}

	// Use strict decoding options to prevent security vulnerabilities
	decoder := yaml.NewDecoder(strings.NewReader(string(payloadBytes)))
	decoder.SetStrict(true)

	var data Payload
	err = decoder.Decode(&data)
	if err != nil {
		http.Error(w, "Failed to parse payload", http.StatusBadRequest)
		return
	}

	// Sanitize the name field
	sanitizedName := html.EscapeString(data.Name)

	w.Header().Set("Content-Type", "application/json")
	jsonResponse, err := json.MarshalIndent(map[string]string{"name": sanitizedName}, "", "  ")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "%s\n", jsonResponse)
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
