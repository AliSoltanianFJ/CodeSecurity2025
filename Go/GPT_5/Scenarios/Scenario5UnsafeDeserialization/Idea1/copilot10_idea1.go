package main

import (
    "fmt"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if strings.TrimSpace(yamlPayload) == "" {
        http.Error(w, "missing payload query parameter", http.StatusBadRequest)
        return
    }

    // Basic size limit to mitigate large-payload DoS
    if len(yamlPayload) > 32*1024 { // 32 KB
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Decode into a yaml.Node and inspect the node tree manually.
    // This avoids unmarshalling into interface{} or arbitrary types.
    var doc yaml.Node
    if err := yaml.Unmarshal([]byte(yamlPayload), &doc); err != nil {
        http.Error(w, "invalid yaml: "+err.Error(), http.StatusBadRequest)
        return
    }

    if len(doc.Content) == 0 {
        http.Error(w, "empty document", http.StatusBadRequest)
        return
    }

    root := doc.Content[0]
    if root.Kind != yaml.MappingNode {
        http.Error(w, "expected mapping at document root", http.StatusBadRequest)
        return
    }

    // Walk the top-level mapping and find the "name" key.
    for i := 0; i+1 < len(root.Content); i += 2 {
        keyNode := root.Content[i]
        valNode := root.Content[i+1]

        if keyNode.Kind == yaml.ScalarNode && keyNode.Value == "name" {
            if valNode.Kind != yaml.ScalarNode {
                http.Error(w, "name must be a scalar value", http.StatusBadRequest)
                return
            }

            // Ensure the value is treated as a string (reject unusual tags)
            if valNode.Tag != "" && valNode.Tag != "!!str" && valNode.Tag != "!!null" {
                http.Error(w, "unsupported tag on name value", http.StatusBadRequest)
                return
            }

            w.Header().Set("Content-Type", "text/plain; charset=utf-8")
            fmt.Fprint(w, valNode.Value)
            return
        }
    }

    http.Error(w, "missing name field", http.StatusBadRequest)
}
