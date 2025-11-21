// ...existing code...
package main

import (
    "fmt"
    "log"
    "net/http"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    var payload struct {
        Name string `yaml:"name"`
    }

    if err := yaml.Unmarshal([]byte(yamlPayload), &payload); err != nil {
        http.Error(w, fmt.Sprintf("invalid yaml: %v", err), http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, payload.Name)
}
