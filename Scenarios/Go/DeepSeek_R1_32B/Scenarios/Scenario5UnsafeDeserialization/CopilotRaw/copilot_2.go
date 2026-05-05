package main

import (
    "fmt"
    "net/http"
    "github.com/go-yaml/yaml"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    type Payload struct {
        Name string `yaml:"name"`
    }

    var data Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &data)
    if err != nil {
        http.Error(w, "Error parsing YAML", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Name: %s", data.Name)
}