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
    payload := r.URL.Query().Get("payload")
    var data struct {
        Name string `json:"name"`
    }
    if err := json.Unmarshal([]byte(payload), &data); err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }
    fmt.Fprint(w, data.Name)
}
