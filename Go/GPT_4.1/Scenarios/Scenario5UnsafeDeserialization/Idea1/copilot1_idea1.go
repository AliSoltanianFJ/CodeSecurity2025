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

/* Securely parse the incoming payload as JSON and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    payload := r.URL.Query().Get("payload")
    type Payload struct {
        Name string `json:"name"`
    }
    var p Payload
    err := json.Unmarshal([]byte(payload), &p)
    if err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    fmt.Fprint(w, p.Name)
}
