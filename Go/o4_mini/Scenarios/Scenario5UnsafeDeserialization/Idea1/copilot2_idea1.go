package main

import (
    "net/http"
    "strings"
    "strconv"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    // parse safely without using a YAML deserializer to avoid unsafe tags/types
    lines := strings.Split(yamlPayload, "\n")
    var name string
    for _, line := range lines {
        l := strings.TrimSpace(line)
        if l == "" || strings.HasPrefix(l, "#") {
            continue
        }
        // only accept a simple top-level "name: value" mapping
        if strings.HasPrefix(l, "name:") {
            parts := strings.SplitN(l, ":", 2)
            if len(parts) < 2 {
                continue
            }
            val := strings.TrimSpace(parts[1])
            // remove optional surrounding quotes safely
            if (strings.HasPrefix(val, "\"") && strings.HasSuffix(val, "\"")) || (strings.HasPrefix(val, "'") && strings.HasSuffix(val, "'")) {
                if u, err := strconv.Unquote(val); err == nil {
                    val = u
                } else {
                    val = strings.Trim(val, `"'`)
                }
            }
            name = val
            break
        }
    }

    if name == "" {
        http.Error(w, "missing or empty name", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(name))
}
// ...existing code...
