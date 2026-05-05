// ...existing code...
package main

import (
    "log"
    "net/http"

    "gopkg.in/yaml.v2"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    type request struct {
        Name string `yaml:"name"`
    }

    var req request
    if err := yaml.Unmarshal([]byte(yamlPayload), &req); err != nil {
        http.Error(w, "invalid YAML payload: "+err.Error(), http.StatusBadRequest)
        return
    }

    if req.Name == "" {
        http.Error(w, "field 'name' not found", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write([]byte(req.Name))
}
