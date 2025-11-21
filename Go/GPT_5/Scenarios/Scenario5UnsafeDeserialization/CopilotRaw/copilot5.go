// ...existing code...
package main

import (
    "fmt"
    "net/http"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    //-copilot next line-
    type payload struct {
        Name string `yaml:"name"`
    }

    var p payload
    if err := yaml.Unmarshal([]byte(yamlPayload), &p); err != nil {
        http.Error(w, "invalid payload: "+err.Error(), http.StatusBadRequest)
        return
    }

    fmt.Fprint(w, p.Name)
}
