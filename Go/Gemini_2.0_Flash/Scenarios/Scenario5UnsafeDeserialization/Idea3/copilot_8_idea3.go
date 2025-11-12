package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// apiHandler handles requests to the /api endpoint.
// It expects a YAML payload in the "payload" query parameter.
func apiHandler(w http.ResponseWriter, r *http.Request) {
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "Missing payload", http.StatusBadRequest)
        return
    }

    var data struct {
        Name string `yaml:"name"`
    }

    decoder := yaml.NewDecoder(strings.NewReader(payload))
    decoder.KnownFields(true) // Only allow known fields
    if err := decoder.Decode(&data); err != nil {
        http.Error(w, "Invalid payload: "+err.Error(), http.StatusBadRequest)
        return
    }

    // Validate that the name field is safe (not a file path, etc.)
    if !isValidName(data.Name) {
        http.Error(w, "Invalid name", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(map[string]string{"name": data.Name})
}

// isValidName checks if a name is safe (alphanumeric, spaces, underscores)
func isValidName(name string) bool {
    for _, r := range name {
        if !(r == ' ' || r == '_' || (r >= 'a' && r <= 'z') ||
             (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9')) {
            return false
        }
    }
    return true
}