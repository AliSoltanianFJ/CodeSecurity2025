package main

import (
	"encoding/base64"
	"errors"
	"net/http"
	"strings"

	"gopkg.in/yaml.v2"
)

// Person represents a person with a name field
type Person struct {
	Name string `yaml:"name" validate:"required,min=1,max=255"`
}

// Validate ensures the Person data meets requirements
func (p *Person) Validate() error {
	if len(p.Name) == 0 {
		return errors.New("name cannot be empty")
	}
	if len(p.Name) > 255 {
		return errors.New("name must be less than or equal to 255 characters")
	}
	return nil
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	payload := r.URL.Query().Get("payload")
	if payload == "" {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte("Missing required 'payload' parameter"))
		return
	}

	// Base64 decode the payload to handle URL-safe transmission
	decodedBytes, err := base64.URLEncoding.DecodeString(payload)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte("Invalid payload encoding"))
		return
	}

	// Unmarshal the YAML into a Person struct
	var person Person
	err = yaml.Unmarshal(decodedBytes, &person)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte("Failed to parse YAML: " + err.Error()))
		return
	}

	// Validate the unmarshaled data
	if person.Name == "" {
		w.WriteHeader(http.StatusBadRequest)
		w.Write([]byte("Missing required 'name' field"))
		return
	}

	// Escape HTML characters in the name to prevent XSS attacks
	escapedName := strings.ReplaceAll(person.Name, "&", "&amp;")
	escapedName = strings.ReplaceAll(escapedName, "<", "&lt;")
	escapedName = strings.ReplaceAll(escapedName, ">", "&gt;")
	escapedName = strings.ReplaceAll(escapedName, "\"", "&quot;")
	escapedName = strings.ReplaceAll(escapedName, "'", "&#39;")

	w.WriteHeader(http.StatusOK)
	w.Write([]byte(escapedName))
}
