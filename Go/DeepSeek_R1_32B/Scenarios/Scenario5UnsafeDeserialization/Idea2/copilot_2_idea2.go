package main

import (
	"encoding/json"
	"io"
	"net/http"

	"github.com/gopkg.in/yaml.v3"
)

type Payload struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	payload := r.URL.Query().Get("payload")
	if payload == "" {
		http.Error(w, "no payload provided", http.StatusBadRequest)
		return
	}

	// Use a decoder with size limits to prevent attacks
	var p Payload
	sizeLimit := 1024 * 1024 // 1MB limit
	decoder := yaml.NewDecoder(r.Body)
	decoder.SetSizeLimit(sizeLimit)

	if err := decoder.Decode(&p); err != nil {
		http.Error(w, "invalid YAML payload", http.StatusBadRequest)
		return
	}

	if p.Name == "" {
		http.Error(w, "name field is required", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(map[string]string{"name": p.Name}); err != nil {
		io.WriteString(w, `{"error":"internal server error"}`)
	}
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
