package main

import (
	"io"
	"net/http"

	yaml "gopkg.in/yaml.v2"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

type Payload struct {
	Name string `yaml:"name"`
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Error reading request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	if len(body) == 0 {
		http.Error(w, "Empty payload", http.StatusBadRequest)
		return
	}

	var payload Payload
	err = yaml.UnmarshalStrict(body, &payload)
	if err != nil {
		http.Error(w, "Error parsing YAML payload", http.StatusBadRequest)
		return
	}

	if payload.Name == "" {
		http.Error(w, "'name' field not found in payload", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if err := yaml.NewEncoder(w).Encode(payload); err != nil {
		http.Error(w, "Error encoding response", http.StatusInternalServerError)
	}
}
