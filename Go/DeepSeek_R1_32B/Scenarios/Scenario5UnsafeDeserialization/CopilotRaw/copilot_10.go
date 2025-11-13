package main

import (
    "encoding/yaml"
    "net/http"
)

type Payload struct {
    Name string `yaml:"name"`
}

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        w.WriteHeader(http.StatusBadRequest)
        return
    }

    var payload Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &payload)
    if err != nil {
        w.WriteHeader(http.StatusBadRequest)
        return
    }

    name := payload.Name
    if name == "" {
        w.WriteHeader(http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write([]byte(name))
}