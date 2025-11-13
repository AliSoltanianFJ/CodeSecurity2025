package main

import (
    "fmt"
    "net/http"
    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

type Data struct {
    Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "No payload provided")
        return
    }
    
    var data Data
    err := yaml.Unmarshal([]byte(yamlPayload), &data)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Error parsing YAML: %v", err)
        return
    }

    fmt.Fprintf(w, "Name: %s", data.Name)
}