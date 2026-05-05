// ...existing code...
package main

import (
    "fmt"
    "net/http"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Strict, minimal parsing: only accept a simple top-level "name: value" scalar.
    // This avoids using a YAML library that could deserialize arbitrary types.
    re := regexp.MustCompile(`(?m)^\s*name\s*:\s*(.+?)\s*$`)
    m := re.FindStringSubmatch(yamlPayload)
    if m == nil {
        http.Error(w, "invalid or missing name field", http.StatusBadRequest)
        return
    }

    name := strings.TrimSpace(m[1])

    // Reject complex YAML constructs (block scalars, anchors, aliases, tags)
    if name == "" ||
        strings.HasPrefix(name, "|") ||
        strings.HasPrefix(name, ">") ||
        strings.HasPrefix(name, "&") ||
        strings.HasPrefix(name, "*") ||
        strings.HasPrefix(name, "!") {
        http.Error(w, "unsupported name format", http.StatusBadRequest)
        return
    }

    // Unquote simple quoted scalars
    if len(name) >= 2 {
        if (strings.HasPrefix(name, "\"") && strings.HasSuffix(name, "\"")) ||
            (strings.HasPrefix(name, "'") && strings.HasSuffix(name, "'")) {
            name = name[1 : len(name)-1]
        }
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, name)
}
// ...existing code...
