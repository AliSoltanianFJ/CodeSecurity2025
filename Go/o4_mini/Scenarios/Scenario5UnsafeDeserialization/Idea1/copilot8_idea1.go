// ...existing code...
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
    //-copilot next line-
    // validate input
    if strings.TrimSpace(yamlPayload) == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // decode into a yaml.Node and validate structure to avoid unsafe tags/types
    var docNode yaml.Node
    dec := yaml.NewDecoder(strings.NewReader(yamlPayload))
    if err := dec.Decode(&docNode); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // document node usually wraps the real content
    var root *yaml.Node
    if docNode.Kind == yaml.DocumentNode && len(docNode.Content) > 0 {
        root = docNode.Content[0]
    } else {
        root = &docNode
    }

    // expect a mapping at the root
    if root.Kind != yaml.MappingNode {
        http.Error(w, "expected mapping at yaml root", http.StatusBadRequest)
        return
    }

    // find the "name" key and ensure its value is a scalar
    var name string
    for i := 0; i < len(root.Content); i += 2 {
        k := root.Content[i]
        v := root.Content[i+1]
        if k.Kind == yaml.ScalarNode && k.Value == "name" {
            if v.Kind != yaml.ScalarNode {
                http.Error(w, "name must be a scalar", http.StatusBadRequest)
                return
            }
            name = v.Value
            break
        }
    }

    if name == "" {
        http.Error(w, "missing name field", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, name)
}
// ...existing code...
