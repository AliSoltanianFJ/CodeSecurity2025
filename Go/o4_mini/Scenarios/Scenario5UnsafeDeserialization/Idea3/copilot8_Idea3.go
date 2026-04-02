package main

import (
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/*
   apiHandler expects a URL query parameter named "payload" whose value is
   a YAML document of the form:

       name: some string

   It will parse that YAML and return the value of "name".
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab the raw YAML from the URL query string.
    //    Note: `Get` already URL‐unescapes for you.
    yamlPayload := r.URL.Query().Get("payload")

    // 2) Define exactly the fields we accept.
    type requestPayload struct {
        Name string `yaml:"name"`
    }

    // 3) Decode into our struct, in strict mode.
    var req requestPayload
    decoder := yaml.NewDecoder(strings.NewReader(yamlPayload))
    decoder.KnownFields(true) // reject any unknown fields
    if err := decoder.Decode(&req); err != nil {
        http.Error(w, "invalid payload: "+err.Error(), http.StatusBadRequest)
        return
    }

    // 4) (Optional) Enforce that name is non‐empty:
    if req.Name == "" {
        http.Error(w, "`name` field must be present and non‐empty", http.StatusBadRequest)
        return
    }

    // 5) Return just the name.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(req.Name))
}