// ...existing code...
package main

import (
    "fmt"
    "log"
    "net/http"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/api", apiHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    // Basic validation: require payload, limit size, and disallow multiline input to avoid
    // complex YAML features (anchors, tags, custom types).
    if yamlPayload == "" {
        http.Error(w, "missing payload query parameter", http.StatusBadRequest)
        return
    }
    if len(yamlPayload) > 4096 {
        http.Error(w, "payload too large", http.StatusBadRequest)
        return
    }
    if strings.ContainsAny(yamlPayload, "\r\n") {
        http.Error(w, "multiline payloads are not allowed", http.StatusBadRequest)
        return
    }

    // Strict, minimal parsing: only accept a single top-level "name: <value>" line.
    // This avoids using a YAML deserializer that could instantiate arbitrary types.
    re := regexp.MustCompile(`^[ \t]*name:[ \t]*(.*)$`)
    m := re.FindStringSubmatch(yamlPayload)
    if len(m) < 2 {
        http.Error(w, "name field not found or payload malformed", http.StatusBadRequest)
        return
    }

    val := strings.TrimSpace(m[1])

    // Remove inline comment if present (simple ' # comment' pattern).
    if idx := strings.Index(val, " #"); idx != -1 {
        val = strings.TrimSpace(val[:idx])
    }

    // If value is quoted with matching single or double quotes, strip them.
    if len(val) >= 2 {
        if (val[0] == '"' && val[len(val)-1] == '"') || (val[0] == '\'' && val[len(val)-1] == '\'') {
            val = val[1 : len(val)-1]
        }
    }

    if val == "" {
        http.Error(w, "name value is empty", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, val)
}
// ...existing code...
