package main

import (
	"io"
	"io/ioutil"
	"net/http"

	"github.com/go-yaml/yaml"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	body, err := ioutil.ReadAll(io.LimitReader(r.Body, 1<<20))
	if err != nil {
		http.Error(w, "Error reading request body", http.StatusBadRequest)
		return
	}
	defer r.Body.Close()

	if len(body) == 0 {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	var data struct {
		Name string `yaml:"name"`
	}

	err = yaml.Unmarshal(body, &data)
	if err != nil {
		http.Error(w, "Invalid YAML payload or missing 'name' field", http.StatusBadRequest)
		return
	}

	if data.Name == "" {
		http.Error(w, "'name' field is required", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	response := map[string]string{
		"name": data.Name,
	}
	json.NewEncoder(w).Encode(response)
}
