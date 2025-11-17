package main

import (
	"encoding/json"
	"fmt"
	"html"
	"io"
	"net/http"
	"reflect"

	"gopkg.in/yaml.v3"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	// Get the YAML payload from the request body or query parameter
	var yamlPayload string

	if r.Method == "POST" || r.Method == "PUT" || r.Method == "PATCH" {
		body, err := io.ReadAll(r.Body)
		if err != nil {
			http.Error(w, "Failed to read request body", http.StatusBadRequest)
			return
		}
		defer r.Body.Close()
		yamlPayload = string(body)
	} else {
		yamlPayload = r.URL.Query().Get("payload")
	}

	// Validate payload size to prevent potential buffer overflow attacks
	if len(yamlPayload) > 1024*1024 { // Limit payload to 1MB
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	// Deserialize the YAML payload into a map
	var data map[string]interface{}
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, "Invalid YAML format", http.StatusBadRequest)
		return
	}

	// Ensure we're using a safe unmarshaler with checks for valid YAML constructs
	if _, ok := data["name"]; !ok {
		http.Error(w, "Missing 'name' field in payload", http.StatusBadRequest)
		return
	}

	// Clean and sanitize the name to prevent XSS or injection attacks
	name, err := sanitizeInput(fmt.Sprintf("%v", data["name"]))
	if err != nil {
		http.Error(w, "Invalid 'name' field value", http.StatusBadRequest)
		return
	}

	// Return the sanitized name in JSON format
	response := map[string]string{
		"name": name,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(response); err != nil {
		http.Error(w, "Failed to encode response", http.StatusInternalServerError)
		return
	}
}

func sanitizeInput(input string) (string, error) {
	// Perform input sanitization here
	// For example, escape HTML characters to prevent XSS attacks
	if reflect.TypeOf(input).Kind() != reflect.String {
		return "", fmt.Errorf("invalid type")
	}

	escaped := html.EscapeString(input)
	if escaped == "" {
		return "", fmt.Errorf("empty string after sanitization")
	}
	return escaped, nil
}
