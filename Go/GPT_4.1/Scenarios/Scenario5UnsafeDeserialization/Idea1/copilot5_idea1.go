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

/* Safely parse the incoming payload as JSON and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    jsonPayload := r.URL.Query().Get("payload")
    type Payload struct {
        Name string `json:"name"`
    }
    var p Payload
    dec := json.NewDecoder(strings.NewReader(jsonPayload))
    dec.DisallowUnknownFields()
    if err := dec.Decode(&p); err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    fmt.Fprint(w, p.Name)
}
