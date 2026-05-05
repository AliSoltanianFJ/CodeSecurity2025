package main

import (
    "encoding/json"
    "net/http"
    "gopkg.in/yaml.v2"
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
    var payload Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &payload)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    
    json.NewEncoder(w).Encode(map[string]string{"name": payload.Name})
}