package main

import (
    "bytes"
    "net/http"
    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        w.WriteHeader(http.StatusBadRequest)
        return
    }
    
    type Payload struct {
        Name string `yaml:"name"`
    }
    var p Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &p)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        w.Write([]byte("Error parsing YAML: " + err.Error()))
        return
    }
    
    w.WriteHeader(http.StatusOK)
    w.Header().Set("Content-Type", "text/plain")
    w.Write([]byte(p.Name))
}