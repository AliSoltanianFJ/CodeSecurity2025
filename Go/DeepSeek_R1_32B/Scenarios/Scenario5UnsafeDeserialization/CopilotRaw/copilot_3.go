package main

import (
    "encoding/yaml"
    "log"
    "net/http"
)

type Payload struct {
    Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "No payload provided", http.StatusBadRequest)
        return
    }

    var payload Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &payload)
    if err != nil {
        log.Printf("Error parsing YAML: %v", err)
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    if payload.Name == "" {
        http.Error(w, "Name field not found in payload", http.StatusBadRequest)
        return
    }

    w.Write([]byte(payload.Name))
}

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}