package main

import (
    "encoding/json"
    "io"
    "net/http"
    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "no payload provided", http.StatusBadRequest)
        return
    }
    
    var data map[string]interface{}
    err := yaml.Unmarshal([]byte(yamlPayload), &data)
    if err != nil {
        http.Error(w, "error parsing yaml", http.StatusInternalServerError)
        return
    }

    name, ok := data["name"]
    if !ok {
        http.Error(w, "no name field found", http.StatusBadRequest)
        return
    }

    response := map[string]string{
        "name": name.(string),
    }
    
    w.Header().Set("Content-Type", "application/json")
    if err := json.NewEncoder(w).Encode(response); err != nil {
        io.WriteString(w, `{"error": "internal server error"}`)
    }
}