// ...existing code...
package main

import (
    "fmt"
    "net/http"
    "strings"
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
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Safely extract a top-level "name" scalar without using a YAML deserializer.
    var name string
    for _, line := range strings.Split(yamlPayload, "\n") {
        line = strings.TrimSpace(line)
        if strings.HasPrefix(line, "name:") {
            val := strings.TrimSpace(strings.TrimPrefix(line, "name:"))
            // Strip surrounding quotes if present
            if len(val) >= 2 && ((val[0] == '"' && val[len(val)-1] == '"') || (val[0] == '\'' && val[len(val)-1] == '\'')) {
                val = val[1 : len(val)-1]
            }
            name = val
            break
        }
    }

    if name == "" {
        http.Error(w, "name not found", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    fmt.Fprint(w, name)
}
// ...existing code...
