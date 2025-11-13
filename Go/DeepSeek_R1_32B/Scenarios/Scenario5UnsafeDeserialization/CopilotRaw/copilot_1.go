package main

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "net/http"
    "github.com/go-yaml/yaml"
)

type Payload struct {
    Name string `yaml:"name"`
}

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "No payload provided", http.StatusBadRequest)
        return
    }
    
    var payload Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &payload)
    if err != nil {
        http.Error(w, fmt.Sprintf("Error parsing YAML: %v", err), http.StatusBadRequest)
        return
    }
    
    w.Header().Set("Content-Type", "application/json")
    if err := json.NewEncoder(w).Encode(map[string]string{"name": payload.Name}); err != nil {
        http.Error(w, fmt.Sprintf("Error encoding response: %v", err), http.StatusInternalServerError)
    }
}