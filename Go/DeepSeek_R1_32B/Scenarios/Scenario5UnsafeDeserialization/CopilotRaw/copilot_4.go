package main

import (
    "encoding/json"
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
    
    var payload Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &payload)
    if err != nil {
        http.Error(w, "Invalid YAML payload or missing 'name' field", http.StatusBadRequest)
        return
    }
    
    response := map[string]string{
        "name": payload.Name,
    }
    
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)
}