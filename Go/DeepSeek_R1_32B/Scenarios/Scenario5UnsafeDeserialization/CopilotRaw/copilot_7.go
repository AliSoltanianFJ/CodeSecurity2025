package main

import (
    "encoding/json"
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
    
    type Payload struct {
        Name string `yaml:"name"`
    }
    
    var payload Payload
    if err := yaml.Unmarshal([]byte(yamlPayload), &payload); err != nil {
        http.Error(w, "Error parsing YAML", http.StatusInternalServerError)
        return
    }
    
    response := map[string]string{
        "name": payload.Name,
    }
    
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)
}