package main

import (
    "fmt"
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
    // ...existing code...

    type payload struct {
        Name string `yaml:"name"`
    }

    var p payload
    if err := yaml.Unmarshal([]byte(yamlPayload), &p); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, p.Name)

    // ...existing code...
}
