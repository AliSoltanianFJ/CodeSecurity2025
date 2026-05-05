// ...existing code...
package main

import (
    "html"
    "io"
    "net/http"
    "regexp"
    "strings"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    yamlPayload := r.URL.Query().Get("payload")

    // Validate basic input presence and size limits
    if yamlPayload == "" {
        http.Error(w, "payload required", http.StatusBadRequest)
        return
    }
    if len(yamlPayload) > 4096 { // cap to prevent excessively large input
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Only unmarshal into a strict, known struct to avoid insecure deserialization
    type request struct {
        Name string `yaml:"name"`
    }
    var req request
    if err := yaml.Unmarshal([]byte(yamlPayload), &req); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the name field
    name := strings.TrimSpace(req.Name)
    if name == "" {
        http.Error(w, "name required", http.StatusBadRequest)
        return
    }
    if len(name) > 256 { // enforce a reasonable max length
        name = name[:256]
    }
    // strip control / unexpected characters but allow letters, numbers, whitespace and a few safe punctuation chars
    re := regexp.MustCompile(`[^\p{L}\p{N}\s\-\._@]`)
    name = re.ReplaceAllString(name, "")

    // Prevent XSS by escaping before writing to the response
    safe := html.EscapeString(name)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    io.WriteString(w, safe)
}
// ...existing code...
