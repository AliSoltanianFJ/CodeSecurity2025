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
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload query parameter", http.StatusBadRequest)
        return
    }

    // Very small, whitelist-style parser: only allow a single top-level "name: value" scalar line
    // Reject any other YAML constructs (anchors, tags, sequences, mappings, block scalars, etc).
    nameLineRe := regexp.MustCompile(`^name\s*:\s*(.+)$`)

    var found bool
    var nameVal string

    for _, rawLine := range strings.Split(yamlPayload, "\n") {
        line := strings.TrimSpace(rawLine)
        if line == "" || strings.HasPrefix(line, "#") {
            // allow blank lines and comments
            continue
        }

        // Disallow common YAML constructs that could lead to complex deserialization
        if strings.HasPrefix(line, "-") || strings.Contains(line, "!!") || strings.ContainsAny(line, "&*|>{}[]") {
            http.Error(w, "unsupported or unsafe YAML constructs in payload", http.StatusBadRequest)
            return
        }

        if matches := nameLineRe.FindStringSubmatch(line); matches != nil {
            if found {
                http.Error(w, "multiple fields not allowed", http.StatusBadRequest)
                return
            }
            found = true
            nameVal = strings.TrimSpace(matches[1])
            // strip surrounding quotes if present
            if len(nameVal) >= 2 {
                if (nameVal[0] == '"' && nameVal[len(nameVal)-1] == '"') || (nameVal[0] == '\'' && nameVal[len(nameVal)-1] == '\'') {
                    nameVal = nameVal[1 : len(nameVal)-1]
                }
            }
            continue
        }

        // If we reach here, there's a non-comment/non-empty line that isn't a simple "name: ..." line
        http.Error(w, "unsupported YAML content; only a single 'name: value' line is accepted", http.StatusBadRequest)
        return
    }

    if !found {
        http.Error(w, "name field not found", http.StatusBadRequest)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, nameVal)
}
