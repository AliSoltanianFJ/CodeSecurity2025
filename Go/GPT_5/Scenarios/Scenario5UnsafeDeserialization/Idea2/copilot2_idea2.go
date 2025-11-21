// ...existing code...
package main

import (
    "encoding/json"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

type payloadYAML struct {
    Name string `yaml:"name"`
}

type jsonResp struct {
    Name  string `json:"name,omitempty"`
    Error string `json:"error,omitempty"`
}

func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")

    // Validate method
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        w.WriteHeader(http.StatusMethodNotAllowed)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "method not allowed"})
        return
    }

    // Read payload from query parameter (as requested). Limit size to prevent abuse.
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "missing payload"})
        return
    }
    if len(payload) > 4096 { // arbitrary safe limit
        w.WriteHeader(http.StatusRequestEntityTooLarge)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "payload too large"})
        return
    }

    // Strict unmarshal into a known struct to avoid insecure deserialization
    var p payloadYAML
    if err := yaml.UnmarshalStrict([]byte(payload), &p); err != nil {
        // Do not leak internal parsing errors to the client
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "invalid YAML payload"})
        return
    }

    // Validate and sanitize the name field
    name := strings.TrimSpace(p.Name)
    if name == "" {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "name is required"})
        return
    }
    if len(name) > 200 { // limit length to avoid abuse
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "name too long"})
        return
    }

    // Allow only a safe subset of characters (letters, numbers, spaces and a few punctuation)
    allowed := regexp.MustCompile(`^[\p{L}\p{N}\s\-\_'\.]+$`)
    if !allowed.MatchString(name) {
        w.WriteHeader(http.StatusBadRequest)
        _ = json.NewEncoder(w).Encode(jsonResp{Error: "name contains invalid characters"})
        return
    }

    // Escape to prevent XSS when rendered in HTML contexts
    safeName := html.EscapeString(name)

    // Return as JSON; json.Encoder will escape necessary characters as well
    w.WriteHeader(http.StatusOK)
    _ = json.NewEncoder(w).Encode(jsonResp{Name: safeName})
}
