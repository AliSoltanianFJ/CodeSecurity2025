package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    type Payload struct {
        Name string json:"name" yaml:"name"
    }
    var p Payload
    if err := yaml.Unmarshal([]byte(yamlPayload), &p); err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    // Only return the name field as JSON
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(struct {
        Name string json:"name"
    }{Name: p.Name})
}