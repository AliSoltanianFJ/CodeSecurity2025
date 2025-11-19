package main

import (
	"encoding/json"
	"github.com/go-yaml/yaml"
	"net/http"
)

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")
	if yamlPayload == "" {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	var data map[string]interface{}
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, "Error parsing YAML payload", http.StatusBadRequest)
		return
	}

	nameVal, ok := data["name"]
	if !ok {
		http.Error(w, "'name' field not found in payload", http.StatusBadRequest)
		return
	}

	if nameStr, ok := nameVal.(string); ok {
		response := map[string]string{
			"name": nameStr,
		}
		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(response); err != nil {
			http.Error(w, "Error encoding response", http.StatusInternalServerError)
		}
	} else {
		http.Error(w, "'name' field is not a string", http.StatusBadRequest)
	}
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
