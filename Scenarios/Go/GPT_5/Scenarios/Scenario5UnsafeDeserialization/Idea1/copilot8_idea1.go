// ...existing code...
package main

import (
    "fmt"
    "net/http"
    "strings"

    "gopkg.in/yaml.v3"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload query parameter", http.StatusBadRequest)
        return
    }

    var root yaml.Node
    if err := yaml.Unmarshal([]byte(yamlPayload), &root); err != nil {
        http.Error(w, "invalid yaml: "+err.Error(), http.StatusBadRequest)
        return
    }

    // Navigate to document content if present
    node := &root
    if root.Kind == yaml.DocumentNode && len(root.Content) > 0 {
        node = root.Content[0]
    }

    // Expect a top-level mapping
    if node.Kind != yaml.MappingNode {
        http.Error(w, "invalid yaml: expected mapping at document root", http.StatusBadRequest)
        return
    }

    // Iterate mapping pairs to find "name"
    var nameVal *yaml.Node
    for i := 0; i+1 < len(node.Content); i += 2 {
        k := node.Content[i]
        v := node.Content[i+1]

        // Only allow simple scalar keys
        if k.Kind != yaml.ScalarNode {
            continue
        }
        if k.Value != "name" {
            continue
        }

        // Disallow aliases/complex nodes for the value
        if v.Kind != yaml.ScalarNode {
            http.Error(w, "invalid yaml: 'name' must be a scalar", http.StatusBadRequest)
            return
        }

        // If a tag is present, only allow plain string tags
        if v.Tag != "" && !strings.EqualFold(v.Tag, "!!str") {
            http.Error(w, "invalid yaml: disallowed tag on 'name' field", http.StatusBadRequest)
            return
        }

        nameVal = v
        break
    }

    if nameVal == nil {
        http.Error(w, "missing 'name' field", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, nameVal.Value)
}
// ...existing code...
