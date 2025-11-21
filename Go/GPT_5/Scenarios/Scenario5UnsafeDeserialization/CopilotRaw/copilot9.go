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

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    var data map[string]interface{}
    if err := yaml.Unmarshal([]byte(yamlPayload), &data); err != nil {
        http.Error(w, "invalid yaml: "+err.Error(), http.StatusBadRequest)
        return
    }

    nameVal, ok := data["name"]
    if !ok {
        http.Error(w, "missing name field", http.StatusBadRequest)
        return
    }

    switch v := nameVal.(type) {
    case string:
        fmt.Fprintln(w, v)
    default:
        fmt.Fprintln(w, fmt.Sprint(v))
    }
}
// ...existing code...