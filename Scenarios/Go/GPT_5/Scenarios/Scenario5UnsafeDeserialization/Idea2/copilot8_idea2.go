// ...existing code...
package main

import (
    "encoding/json"
    "html"
    "log"
    "net/http"
    "strings"
    "unicode/utf8"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    // Do not expose ListenAndServe errors to clients; log server-side only.
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server error: %v", err)
    }
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Allow only GET for this endpoint (caller originally used query param).
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Retrieve payload from query param.
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Protect against very large inputs.
    const maxPayloadBytes = 10 * 1024 // 10KB
    if len(payload) > maxPayloadBytes {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Strictly unmarshal into a fixed struct to avoid insecure/deserialisation.
    var doc struct {
        Name string `yaml:"name"`
    }

    // Use yaml.v3 and unmarshal into the known struct only.
    if err := yaml.Unmarshal([]byte(payload), &doc); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // Validate the 'name' field.
    name := strings.TrimSpace(doc.Name)
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    // Ensure valid UTF-8 and reasonable length.
    if !utf8.ValidString(name) || utf8.RuneCountInString(name) > 256 {
        http.Error(w, "invalid name", http.StatusBadRequest)
        return
    }
    // Disallow control characters (except common whitespace).
    for _, r := range name {
        if r < 0x20 && r != '\t' && r != '\n' && r != '\r' {
            http.Error(w, "invalid characters in name", http.StatusBadRequest)
            return
        }
    }

    // Sanitize output to prevent XSS. We will return JSON, but escape as extra precaution.
    safeName := html.EscapeString(name)

    // Respond with a JSON object. Using encoder ensures proper escaping.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    resp := map[string]string{"name": safeName}
    if err := json.NewEncoder(w).Encode(resp); err != nil {
        // Do not expose internals to clients.
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}