// ...existing code...
package main

import (
    "fmt"
    "log"
    "net/http"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// deserialize the incoming YAML payload and return the field "name"
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    const maxPayloadSize = 4096
    if yamlPayload == "" {
        http.Error(w, "missing payload query parameter", http.StatusBadRequest)
        return
    }
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Safe, minimal extraction: do not perform full YAML deserialization.
    // Look for a top-level "name:" key and extract a simple scalar value.
    var name string
    for _, rawLine := range strings.Split(yamlPayload, "\n") {
        line := strings.TrimSpace(rawLine)
        // Match lines like: name: value  or name: "value" or name: 'value'
        if strings.HasPrefix(line, "name:") {
            val := strings.TrimSpace(strings.TrimPrefix(line, "name:"))
            if val == "" {
                // skip empty or multiline YAML values — require a simple scalar
                continue
            }
            // Remove surrounding quotes if present
            if len(val) >= 2 {
                if (val[0] == '"' && val[len(val)-1] == '"') || (val[0] == '\'' && val[len(val)-1] == '\'') {
                    val = val[1 : len(val)-1]
                }
            }
            name = val
            break
        }
    }

    if name == "" {
        http.Error(w, "name not found or not a simple scalar", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, name)
}
// ...existing code...