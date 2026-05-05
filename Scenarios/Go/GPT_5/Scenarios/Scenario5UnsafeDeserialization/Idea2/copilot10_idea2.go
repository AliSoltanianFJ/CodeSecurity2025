package main

import (
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET for this endpoint (adjust if you expect POST/PUT)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    yamlPayload := r.URL.Query().Get("payload")

    // Validate input presence
    if strings.TrimSpace(yamlPayload) == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Limit payload size to prevent resource exhaustion
    const maxPayloadBytes = 4 * 1024 // 4 KB
    if len(yamlPayload) > maxPayloadBytes {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Reject binary/NUL bytes
    if strings.IndexByte(yamlPayload, 0) != -1 {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Define a strict, known structure for deserialization to avoid unsafe deserialization
    var p struct {
        Name string `yaml:"name"`
    }

    // Use yaml.v3 and unmarshal into the concrete struct only
    if err := yaml.Unmarshal([]byte(yamlPayload), &p); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // Sanitise and validate the name field
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    // Allow Unicode letters, numbers and a small safe set of punctuation
    var validName = regexp.MustCompile(`^[\p{L}\p{N} _\-\.\']+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Prevent XSS by escaping the output and set safe headers
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    safeName := html.EscapeString(name)
    _, _ = w.Write([]byte(safeName))
}
