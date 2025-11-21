// ...existing code...
package main

import (
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
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    const maxPayload = 4096

    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    if len(payload) > maxPayload {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }
    if !utf8.ValidString(payload) {
        http.Error(w, "invalid payload encoding", http.StatusBadRequest)
        return
    }
    payload = strings.TrimSpace(payload)

    type Payload struct {
        Name string `yaml:"name"`
    }

    var p Payload
    // Use a Decoder with KnownFields=true to avoid unexpected fields / types
    dec := yaml.NewDecoder(strings.NewReader(payload))
    dec.KnownFields(true)
    if err := dec.Decode(&p); err != nil {
        http.Error(w, "invalid payload format", http.StatusBadRequest)
        return
    }

    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name missing", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }

    // Reject control characters and newlines to reduce injection/XSS risk
    for _, r := range name {
        if r == '\n' || r == '\r' {
            http.Error(w, "invalid characters in name", http.StatusBadRequest)
            return
        }
        if r != '\t' && (r < 0x20 || r == 0x7f) {
            http.Error(w, "invalid characters in name", http.StatusBadRequest)
            return
        }
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    // Escape the output to prevent XSS if the result is ever rendered in HTML contexts
    _, _ = w.Write([]byte(html.EscapeString(name)))
}
