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
    if err := yaml.Unmarshal([]byte(yamlPayload), &p); err != nil {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }
    // Escape output to prevent XSS
    resp := map[string]string{
        "name": html.EscapeString(p.Name),
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(resp)
}