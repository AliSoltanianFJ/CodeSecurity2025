package main

import (
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
    //-copilot next line-
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    var payload struct {
        Name string `yaml:"name"`
    }

    if err := yaml.Unmarshal([]byte(yamlPayload), &payload); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(payload.Name))
}
