package main

import (
    "net/http"
    "regexp"
    "strconv"
    "strings"
)

// ...existing code...
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

    // Only accept a simple top-level scalar "name: value" on a single line.
    // This avoids invoking a YAML parser and prevents unsafe deserialization features.
    re := regexp.MustCompile(`(?m)^\s*name\s*:\s*(.+)$`)
    m := re.FindStringSubmatch(yamlPayload)
    if len(m) < 2 {
        http.Error(w, "name not found", http.StatusBadRequest)
        return
    }

    raw := strings.TrimSpace(m[1])
    // strip inline comment
    if i := strings.Index(raw, "#"); i != -1 {
        raw = strings.TrimSpace(raw[:i])
    }

    var name string
    if len(raw) >= 2 && ((raw[0] == '"' && raw[len(raw)-1] == '"') || (raw[0] == '\'' && raw[len(raw)-1] == '\'')) {
        if raw[0] == '"' {
            if unq, err := strconv.Unquote(raw); err == nil {
                name = unq
            } else {
                name = raw[1 : len(raw)-1]
            }
        } else { // single-quoted YAML: '' -> '
            inner := raw[1 : len(raw)-1]
            name = strings.ReplaceAll(inner, "''", "'")
        }
    } else {
        name = raw
    }

    // basic validation
    if name == "" {
        http.Error(w, "empty name", http.StatusBadRequest)
        return
    }
    if len(name) > 1000 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(name))
}
// ...existing code...
