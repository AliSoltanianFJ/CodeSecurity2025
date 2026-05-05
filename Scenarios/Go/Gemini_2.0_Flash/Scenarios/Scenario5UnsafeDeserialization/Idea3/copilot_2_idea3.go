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
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "Missing payload", http.StatusBadRequest)
        return
    }

    var data map[string]interface{}
    if err := yaml.Unmarshal([]byte(yamlPayload), &data); err != nil {
        http.Error(w, "Invalid YAML", http.StatusBadRequest)
        return
    }

    name, ok := data["name"].(string)
    if !ok {
        http.Error(w, "Field 'name' missing or not a string", http.StatusBadRequest)
        return
    }

    json.NewEncoder(w).Encode(map[string]string{"name": name})
}