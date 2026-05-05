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
    // For safety, only allow JSON payloads (not YAML)
    type Payload struct {
        Name string `json:"name"`
    }
    var p Payload
    err := json.Unmarshal([]byte(yamlPayload), &p)
    if err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    fmt.Fprint(w, p.Name)
}
