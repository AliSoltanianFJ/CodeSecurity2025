package main

import (
    "encoding/json"
    "gopkg.in/yaml.v3"
    "html"
    "log"
    "net/http"
    "regexp"
    "strings"
)

type payload struct {
    Name string `yaml:"name"`
}

func main() {
    http.HandleFunc("/api", apiHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Allow only GET or POST for this handler
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read payload from query parameter (as specified)
    yamlPayload := strings.TrimSpace(r.URL.Query().Get("payload"))
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Enforce a maximum payload size to avoid excessive memory use
    const maxPayloadLen = 2048
    if len(yamlPayload) > maxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Unmarshal into a strict, known struct to avoid insecure deserialization
    var p payload
    if err := yaml.Unmarshal([]byte(yamlPayload), &p); err != nil {
        log.Printf("yaml unmarshal error: %v", err)
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitise the 'name' field
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "missing name", http.StatusBadRequest)
        return
    }

    // Allow letters, numbers and a small set of safe punctuation, limit length
    var validName = regexp.MustCompile(`^[\p{L}\p{N} _\-\.\,]{1,200}$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid name", http.StatusBadRequest)
        return
    }

    // Escape any HTML chars to prevent XSS
    safeName := html.EscapeString(name)

    // Return JSON with proper Content-Type; encoder also escapes HTML by default
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    resp := map[string]string{"name": safeName}
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        log.Printf("response encode error: %v", err)
    }
}
