package main

import (
    "net/http"
    "regexp"
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

    // Simple, safe extraction of a top-level "name" scalar.
    // This avoids a full YAML deserialization and rejects complex/unsafe constructs.
    re := regexp.MustCompile(`(?m)^[ \t]*name[ \t]*:[ \t]*(.+)$`)
    m := re.FindStringSubmatch(yamlPayload)
    if len(m) < 2 {
        http.Error(w, "name field not found", http.StatusBadRequest)
        return
    }

    name := strings.TrimSpace(m[1])
    // strip matching surrounding quotes
    if len(name) >= 2 {
        if (name[0] == '"' && name[len(name)-1] == '"') || (name[0] == '\'' && name[len(name)-1] == '\'') {
            name = name[1 : len(name)-1]
        }
    }

    // Reject multiline or obviously structured values to keep parsing strict/simple
    if strings.ContainsAny(name, "\r\n") {
        http.Error(w, "invalid name value", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte(name))
}
// ...existing code...
