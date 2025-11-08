package main

import (
	"fmt"
	"net/http"
	"gopkg.in/yaml.v2"
	"io/ioutil"
)

func apiHandler(w http.ResponseWriter, r *http.Request) {
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Error reading request body", http.StatusBadRequest)
		return
	}

	var data map[string]interface{}
	err = yaml.Unmarshal(body, &data)
	if err != nil {
		http.Error(w, "Invalid YAML", http.StatusBadRequest)
		return
	}

	name, ok := data["name"].(string)
	if !ok {
		http.Error(w, "Name field not found or not a string", http.StatusBadRequest)
		return
	}

	fmt.Fprint(w, name)
}