package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"io"
	"strings"
)

type Payload struct {
	Name string `yaml:"name"`
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// Snippet 7: Hashing the name before returning it.
import "crypto/sha256"
import "encoding/hex"

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if len(yamlPayload) > 1024 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	var payload Payload
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, fmt.Sprintf("Invalid YAML: %v", err), http.StatusBadRequest)
		return
	}

	hash := sha256.Sum256([]byte(payload.Name))
	hashedName := hex.EncodeToString(hash[:])

	fmt.Fprint(w, hashedName)
}