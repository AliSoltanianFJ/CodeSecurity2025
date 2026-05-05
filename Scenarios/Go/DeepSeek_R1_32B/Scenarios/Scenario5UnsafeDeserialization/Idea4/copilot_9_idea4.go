package main

import (
	"encoding/base64"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"

	yaml "gopkg.in/yaml.v3"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

type Payload struct {
	Name string `yaml:"name"`
	Data []byte `yaml:"data,omitempty"`
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	payloadParam := r.URL.Query().Get("payload")
	if payloadParam == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Missing required 'payload' parameter.")
		return
	}

	maxPayloadSize := 1024 * 1024 // 1MB
	if len(payloadParam) > maxPayloadSize {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Payload exceeds maximum allowed size of %d bytes.", maxPayloadSize)
		return
	}

	payloadBytes, err := base64.StdEncoding.DecodeString(payloadParam)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid Base64 encoded payload: %v", err)
		return
	}

	var payload Payload
	err = yaml.Unmarshal(payloadBytes, &payload)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "YAML unmarshalling error: %v", err)
		return
	}

	if payload.Name == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Missing 'name' field in YAML payload.")
		return
	}

	w.Write([]byte(strings.TrimSpace(payload.Name)))
}
