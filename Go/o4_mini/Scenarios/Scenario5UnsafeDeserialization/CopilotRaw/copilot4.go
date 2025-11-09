package main

import (
    "net/http"
    "gopkg.in/yaml.v2"
)

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    var payload struct {
        Name string `yaml:"name"`
    }

    if err := yaml.Unmarshal([]byte(yamlPayload), &payload); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte(payload.Name))
}
// ...existing code...
