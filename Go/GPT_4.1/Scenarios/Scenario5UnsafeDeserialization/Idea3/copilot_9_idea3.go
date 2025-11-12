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
        Name string `yaml:"name" json:"name"`
    }
    var p Payload
    err := yaml.Unmarshal([]byte(yamlPayload), &p)
    if err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    json.NewEncoder(w).Encode(map[string]string{"name": p.Name})
}