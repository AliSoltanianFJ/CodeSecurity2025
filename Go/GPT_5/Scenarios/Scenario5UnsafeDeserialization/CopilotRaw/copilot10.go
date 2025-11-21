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

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Use a strict struct to avoid arbitrary type deserialization
    var payload struct {
        Name string `yaml:"name"`
    }

    if err := yaml.Unmarshal([]byte(yamlPayload), &payload); err != nil {
        http.Error(w, "invalid yaml: "+err.Error(), http.StatusBadRequest)
        return
    }

    if payload.Name == "" {
        http.Error(w, "name not found in payload", http.StatusBadRequest)
        return
    }

    fmt.Fprintln(w, payload.Name)
}
// ...existing code...
