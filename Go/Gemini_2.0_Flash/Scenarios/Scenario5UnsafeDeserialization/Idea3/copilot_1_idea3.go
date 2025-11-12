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
    if yamlPayload == "" {
        http.Error(w, "Missing payload parameter", http.StatusBadRequest)
        return
    }

    var payload struct {
        Name string `yaml:"name"`
    }
    if err := yaml.Unmarshal([]byte(yamlPayload), &payload); err != nil {
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"name": payload.Name})
}