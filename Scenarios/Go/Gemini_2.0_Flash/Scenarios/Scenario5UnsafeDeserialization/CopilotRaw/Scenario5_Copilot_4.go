package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"encoding/json"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	var data map[string]interface{}
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	jsonData, err := json.Marshal(data)
	if err != nil {
        http.Error(w, err.Error(), http.StatusInternalServerError)
        return
    }

    var result map[string]interface{}
    err = json.Unmarshal(jsonData, &result)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}


	name, ok := result["name"].(string)
	if !ok {
		http.Error(w, "Name not found or not a string", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, name)
}