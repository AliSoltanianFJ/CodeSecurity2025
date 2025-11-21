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

// ...existing code...
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

    // root should be a document node with at least one content node
    if root.Kind != yaml.DocumentNode || len(root.Content) == 0 {
        http.Error(w, "unexpected yaml structure", http.StatusBadRequest)
        return
    }
    top := root.Content[0]

    // disallow aliases anywhere in the document
    var hasAlias func(n *yaml.Node) bool
    hasAlias = func(n *yaml.Node) bool {
        if n == nil {
            return false
        }
        if n.Kind == yaml.AliasNode {
            return true
        }
        for _, c := range n.Content {
            if hasAlias(c) {
                return true
            }
        }
        return false
    }
    if hasAlias(&root) {
        http.Error(w, "aliases are not allowed", http.StatusBadRequest)
        return
    }

    // expect top-level mapping and look up "name" key only
    if top.Kind != yaml.MappingNode {
        http.Error(w, "expected mapping at document root", http.StatusBadRequest)
        return
    }

    var name string
    found := false
    for i := 0; i+1 < len(top.Content); i += 2 {
        keyNode := top.Content[i]
        valNode := top.Content[i+1]

        // keys must be plain scalars
        if keyNode.Kind != yaml.ScalarNode {
            http.Error(w, "invalid key type", http.StatusBadRequest)
            return
        }
        if keyNode.Value != "name" {
            continue
        }

        // value must be a plain scalar string, not a complex/node tagged type
        if valNode.Kind != yaml.ScalarNode {
            http.Error(w, "name must be a scalar string", http.StatusBadRequest)
            return
        }
        // Accept only explicit string tags or no tag (plain)
        tag := valNode.Tag
        if tag != "" && !strings.HasSuffix(tag, "str") && tag != "!!str" {
            http.Error(w, "name has disallowed tag", http.StatusBadRequest)
            return
        }

        name = valNode.Value
        found = true
        break
    }

    if !found {
        http.Error(w, "missing name field", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, name)
}
