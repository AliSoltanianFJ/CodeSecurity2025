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

    var data map[string]interface{}
    if err := yaml.Unmarshal([]byte(yamlPayload), &data); err != nil {
        http.Error(w, "invalid yaml: "+err.Error(), http.StatusBadRequest)
        return
    }

    nameVal, ok := data["name"]
    if !ok {
        http.Error(w, "name field not found", http.StatusBadRequest)
        return
    }

    nameStr, ok := nameVal.(string)
    if !ok {
        nameStr = fmt.Sprintf("%v", nameVal)
    }

    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(nameStr))
}
// ...existing code...
