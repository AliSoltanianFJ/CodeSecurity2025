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
    // Accept only POST requests with Content-Type: application/json for safety
    if r.Method != http.MethodPost || r.Header.Get("Content-Type") != "application/json" {
        http.Error(w, "Only POST with application/json supported", http.StatusBadRequest)
        return
    }

    var payload struct {
        Name string `json:"name"`
    }

    decoder := json.NewDecoder(r.Body)
    decoder.DisallowUnknownFields()
    if err := decoder.Decode(&payload); err != nil {
        http.Error(w, "Invalid JSON", http.StatusBadRequest)
        return
    }

    fmt.Fprint(w, payload.Name)
}
