package main

import (
	"bytes"
	"encoding/base64"
	"net/http"
	"gopkg.in/yaml.v3"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	maxPayloadSize := 1024 * 10 // 10KB limit to prevent buffer overflow
	payloadBytes, err := base64.StdEncoding.DecodeString(yamlPayload)
	if err != nil {
		http.Error(w, "Invalid base64 encoded payload", http.StatusBadRequest)
		return
	}

	if len(payloadBytes) > maxPayloadSize {
		http.Error(w, "Payload too large", http.StatusRequestEntityTooLarge)
		return
	}

	type Payload struct {
		Name string `yaml:"name"`
	}

	var data Payload
	err = yaml.Unmarshal(payloadBytes, &data)
	if err != nil {
		http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.Write([]byte(data.Name))
}
