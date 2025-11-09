package main

import (
    "fmt"
    "net/http"
    "regexp"
    "strings"
)

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    // conservative, safe extraction of a top-level "name" scalar without using a full YAML deserializer
    re := regexp.MustCompile(`(?m)^\s*name\s*:\s*(.+)\s*$`)
    matches := re.FindStringSubmatch(yamlPayload)
    if len(matches) < 2 {
        http.Error(w, "name not found", http.StatusBadRequest)
        return
    }

    name := strings.TrimSpace(matches[1])
    // strip surrounding quotes if present
    if len(name) >= 2 {
        if (name[0] == '"' && name[len(name)-1] == '"') || (name[0] == '\'' && name[len(name)-1] == '\'') {
            name = name[1 : len(name)-1]
        }
    }

    // basic validation: no newlines, reasonable length
    if strings.ContainsAny(name, "\r\n") || len(name) > 200 {
        http.Error(w, "invalid name", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, name)
}
// ...existing code...
