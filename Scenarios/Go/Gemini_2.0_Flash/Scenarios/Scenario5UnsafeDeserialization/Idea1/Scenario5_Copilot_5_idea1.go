package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"encoding/json"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	var data map[string]interface{}
	
	err := yaml.Unmarshal([]byte(yamlPayload), &data)
	if err != nil {
		http.Error(w, "Invalid YAML", http.StatusBadRequest)
		return
	}

	jsonData, err := json.Marshal(data)
	if err != nil {
		http.Error(w, "Error marshaling to JSON", http.StatusInternalServerError)
		return
	}

	var jsonDataMap map[string]interface{}
	err = json.Unmarshal(jsonData, &jsonDataMap)

	name, ok := jsonDataMap["name"].(string)
	if !ok {
		http.Error(w, "Name field not found or not a string", http.StatusBadRequest)
		return
	}
	fmt.Fprint(w, name)
}