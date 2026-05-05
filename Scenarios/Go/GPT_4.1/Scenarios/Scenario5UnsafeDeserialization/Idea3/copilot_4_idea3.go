package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "Missing payload", http.StatusBadRequest)
        return
    }

    // Define a struct with only the allowed fields to prevent unsafe deserialization
    type SafePayload struct {
        Name string `json:"name"`
    }

    var payload SafePayload
    if err := json.Unmarshal([]byte(yamlPayload), &payload); err != nil {
        http.Error(w, "Invalid payload", http.StatusBadRequest)
        return
    }

    // Output encoding to prevent XSS
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{
        "name": payload.Name,
    })
}