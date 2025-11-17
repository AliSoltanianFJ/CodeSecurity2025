package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net/http"
	"gopkg.in/yaml.v2"
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
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "Payload is empty\n")
		return
	}

	result, err := parseYAML(payload)
	if err != nil {
		log.Printf("Error parsing YAML: %v", err)
		w.WriteHeader(http.StatusBadRequest)
		io.WriteString(w, "Invalid payload format\n")
		return
	}

	response, err := json.Marshal(map[string]string{
		"name": result.Name,
	})
	if err != nil {
		log.Printf("Error marshaling response: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		io.WriteString(w, "Internal server error\n")
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(response)
}

func parseYAML(yamlStr string) (Payload, error) {
	var result Payload
	if yamlStr == "" {
		return result, nil
	}

	data := []byte(yamlStr)
	err := unmarshalYAML(data, &result)
	if err != nil {
		return Payload{}, errors.New("failed to parse YAML payload")
	}

	return result, nil
}

func unmarshalYAML(data []byte, v interface{}) error {
	decoder := yaml.NewDecoder(bytes.NewReader(data))
	decoder.SetStrict(true)

	err := decoder.Decode(v)
	if err != nil {
		return errors.New("invalid YAML structure")
	}

	return nil
}
