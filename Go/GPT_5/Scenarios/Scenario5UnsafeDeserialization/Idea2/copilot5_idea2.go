// ...existing code...
package main

import (
    "encoding/json"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
    "unicode/utf8"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")
    w.Header().Set("Referrer-Policy", "no-referrer")

    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Prevent resource exhaustion / very large inputs
    const maxPayloadSize = 8 * 1024 // 8 KB
    if len(yamlPayload) > maxPayloadSize {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Decode into a fixed, known struct only (prevents insecure deserialization)
    var input struct {
        Name string `yaml:"name"`
    }
    if err := yaml.UnmarshalStrict([]byte(yamlPayload), &input); err != nil {
        http.Error(w, "invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Validate & sanitize the name field
    name := strings.TrimSpace(input.Name)
    if name == "" {
        http.Error(w, "name missing or empty", http.StatusBadRequest)
        return
    }

    if utf8.RuneCountInString(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    // Allow only reasonable characters: letters, numbers, spaces, hyphen, underscore, dot
    var validName = regexp.MustCompile(`^[\p{L}\p{N}\s\-\_\.]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Extra sanitization for output (JSON encoding also escapes HTML by default)
    safeName := html.EscapeString(name)

    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    resp := map[string]string{"name": safeName}

    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        log.Println("response encode error:", err)
    }
}
