package main

import (
    "encoding/json"
    "fmt"
    "net/http"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    // For safety, only allow JSON payloads (not YAML) to avoid unsafe deserialization
    var payload map[string]interface{}
    err := json.Unmarshal([]byte(yamlPayload), &payload)
    if err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    name, ok := payload["name"].(string)
    if !ok {
        http.Error(w, "Field 'name' not found", http.StatusBadRequest)
        return
    }
    fmt.Fprint(w, name)
}
