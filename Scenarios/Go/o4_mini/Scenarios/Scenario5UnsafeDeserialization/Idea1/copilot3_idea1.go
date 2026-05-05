// ...existing code...
package main

import (
    "net/http"
    "fmt"
    "gopkg.in/yaml.v3"
)

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    // parse into a yaml.Node and validate structure to avoid unsafe deserialization
    var root yaml.Node
    if err := yaml.Unmarshal([]byte(yamlPayload), &root); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // get the document content (if present)
    var mapping *yaml.Node
    if root.Kind == yaml.DocumentNode && len(root.Content) > 0 {
        mapping = root.Content[0]
    } else {
        mapping = &root
    }

    if mapping.Kind != yaml.MappingNode {
        http.Error(w, "expected YAML mapping", http.StatusBadRequest)
        return
    }

    // iterate mapping pairs and extract "name" only if it's a plain scalar
    var name string
    for i := 0; i+1 < len(mapping.Content); i += 2 {
        keyNode := mapping.Content[i]
        valNode := mapping.Content[i+1]

        // disallow aliases and require scalar key
        if keyNode.Kind != yaml.ScalarNode || keyNode.Value != "name" {
            continue
        }
        if valNode.Kind != yaml.ScalarNode {
            http.Error(w, "invalid name value", http.StatusBadRequest)
            return
        }
        name = valNode.Value
        break
    }

    if name == "" {
        http.Error(w, "name not found", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, name)
}
// ...existing code...
