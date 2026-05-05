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

// Snippet 4:  Implementing a timeout to prevent long processing times.
import (
	"context"
	"time"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if len(yamlPayload) > 1024 {
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	var payload Payload

	ctx, cancel := context.WithTimeout(context.Background(), 1*time.Second)
	defer cancel()

	done := make(chan error, 1)
	go func() {
		done <- yaml.Unmarshal([]byte(yamlPayload), &payload)
	}()

	select {
	case <-ctx.Done():
		http.Error(w, "Request timeout", http.StatusRequestTimeout)
		return
	case err := <-done:
		if err != nil {
			http.Error(w, fmt.Sprintf("Invalid YAML: %v", err), http.StatusBadRequest)
			return
		}
	}

	fmt.Fprint(w, payload.Name)
}