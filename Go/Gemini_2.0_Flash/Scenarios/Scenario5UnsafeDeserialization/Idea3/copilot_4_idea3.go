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
    http.Error(w, "Payload parameter missing", http.StatusBadRequest)
    return
  }

  var data struct {
    Name string `yaml:"name"`
  }

  decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
  if err := decoder.Decode(&data); err != nil {
    http.Error(w, "Invalid payload", http.StatusBadRequest)
    return
  }

  fmt.Fprint(w, data.Name)
}