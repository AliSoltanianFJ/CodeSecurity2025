package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests for this API endpoint
    if r.Method != http.MethodGet {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "Payload query parameter is missing", http.StatusBadRequest)
        return
    }

    // Define a struct with only the "name" field to be deserialized
    type NameOnly struct {
        Name string `yaml:"name"`
    }
    var nameOnly NameOnly

    // Use DecoderConfig to disallow unknown fields
    decoder := yaml.NewDecoder(bytes.NewReader([]byte(yamlPayload)))
    decoder.KnownFields(true) // Only allow known fields

    if err := decoder.Decode(&nameOnly); err != nil {
        http.Error(w, "Invalid payload: " + err.Error(), http.StatusBadRequest)
        return
    }

    // Output only the "name" field as JSON
    output := struct {
        Name string `json:"name"`
    }{Name: nameOnly.Name}

    w.Header().Set("Content-Type", "application/json")
    if err := json.NewEncoder(w).Encode(output); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
    }
}