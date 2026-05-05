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

    var root yaml.Node
    if err := yaml.Unmarshal([]byte(yamlPayload), &root); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // allow only safe YAML tags
    allowed := map[string]struct{}{
        "!!map":   {},
        "!!str":   {},
        "!!seq":   {},
        "!!int":   {},
        "!!bool":  {},
        "!!null":  {},
        "!!float": {},
    }

    var unsafe bool
    var walk func(*yaml.Node)
    walk = func(n *yaml.Node) {
        if n == nil || unsafe {
            return
        }
        if n.Tag != "" {
            if _, ok := allowed[n.Tag]; !ok {
                unsafe = true
                return
            }
        }
        for _, c := range n.Content {
            walk(c)
            if unsafe {
                return
            }
        }
    }
    walk(&root)
    if unsafe {
        http.Error(w, "unsafe yaml payload", http.StatusBadRequest)
        return
    }

    // locate mapping node (document -> mapping)
    var mapping *yaml.Node
    if root.Kind == yaml.DocumentNode && len(root.Content) > 0 {
        mapping = root.Content[0]
    } else {
        mapping = &root
    }

    if mapping.Kind != yaml.MappingNode {
        http.Error(w, "expected mapping", http.StatusBadRequest)
        return
    }

    var name string
    for i := 0; i < len(mapping.Content); i += 2 {
        k := mapping.Content[i]
        v := mapping.Content[i+1]
        if k.Kind == yaml.ScalarNode && k.Value == "name" {
            if v.Kind == yaml.ScalarNode {
                name = v.Value
            } else {
                // fallback: decode non-scalar into a Go value and stringify
                var tmp interface{}
                if err := v.Decode(&tmp); err == nil {
                    name = fmt.Sprintf("%v", tmp)
                }
            }
            break
        }
    }

    if name == "" {
        http.Error(w, "missing name field", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(name))
}
// ...existing code...
