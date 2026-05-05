// ...existing code...
package main

import (
    "fmt"
    "net/http"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    var rootNode yaml.Node
    if err := yaml.Unmarshal([]byte(yamlPayload), &rootNode); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // get document root node
    var doc *yaml.Node
    if rootNode.Kind == yaml.DocumentNode && len(rootNode.Content) > 0 {
        doc = rootNode.Content[0]
    } else {
        doc = &rootNode
    }

    // expect a mapping at top level
    if doc.Kind != yaml.MappingNode {
        http.Error(w, "invalid yaml payload: expected mapping", http.StatusBadRequest)
        return
    }

    // locate "name" key and ensure its value is a plain/string scalar
    var name string
    for i := 0; i+1 < len(doc.Content); i += 2 {
        key := doc.Content[i]
        val := doc.Content[i+1]
        if key.Kind == yaml.ScalarNode && key.Value == "name" {
            if val.Kind != yaml.ScalarNode {
                http.Error(w, "invalid yaml payload: name must be scalar", http.StatusBadRequest)
                return
            }
            // accept only plain or string-tagged scalars to avoid unsafe tags
            if val.Tag != "!!str" && val.Tag != "" {
                http.Error(w, "invalid yaml payload: name must be a string", http.StatusBadRequest)
                return
            }
            name = val.Value
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
