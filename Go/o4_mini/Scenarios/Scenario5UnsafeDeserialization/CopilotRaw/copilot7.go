package main

import (
    "net/http"

    "gopkg.in/yaml.v2"
)

// ...existing code...

func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    var data map[string]interface{}
    if err := yaml.Unmarshal([]byte(yamlPayload), &data); err != nil {
        http.Error(w, "invalid yaml", http.StatusBadRequest)
        return
    }

    name, _ := data["name"].(string)
    if name == "" {
        http.Error(w, "name not found", http.StatusBadRequest)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte(name))
}

// ...existing code...
