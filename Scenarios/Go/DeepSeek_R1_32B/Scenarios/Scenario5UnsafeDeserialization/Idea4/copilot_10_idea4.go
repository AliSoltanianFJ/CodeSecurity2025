package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"

	"gopkg.in/yaml.v3"
)

type Payload struct {
	Name *string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if yamlPayload == "" {
		http.Error(w, "Missing payload", http.StatusBadRequest)
		return
	}

	var data map[string]interface{}
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, "Invalid YAML format", http.StatusBadRequest)
		return
	}

	payload := Payload{}
	if nameValue, ok := data["name"]; ok {
		nameStr, ok := nameStrConv(nameValue)
		if !ok {
			http.Error(w, "Invalid name field type", http.StatusBadRequest)
			return
		}

		sanitizedName, err := sanitizeName(nameStr)
		if err != nil {
			http.Error(w, "Invalid name value", http.StatusBadRequest)
			return
		}

		payload.Name = &sanitizedName
	} else {
		http.Error(w, "Missing 'name' field in payload", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(payload); err != nil {
		log.Printf("Error encoding response: %v\n", err)
		w.WriteHeader(http.StatusInternalServerError)
	}
}

func nameStrConv(value interface{}) (string, bool) {
	switch v := value.(type) {
	case string:
		return v, true
	case int:
		return fmt.Sprintf("%d", v), true
	default:
		return "", false
	}
}

func sanitizeName(name string) (string, error) {
	if len(name) == 0 {
		return "", fmt.Errorf("name cannot be empty")
	}

	maxLength := 100
	if len(name) > maxLength {
		return "", fmt.Errorf("name exceeds maximum allowed length of %d characters", maxLength)
	}

	if !isPrintableASCII(name) {
		return "", fmt.Errorf("name contains invalid characters")
	}

	return strings.TrimSpace(name), nil
}

func isPrintableASCII(s string) bool {
	for _, c := range s {
		if c < 0x20 || c > 0x7E {
			return false
		}
	}
	return true
}

func main() {
	http.HandleFunc("/api", apiHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}
