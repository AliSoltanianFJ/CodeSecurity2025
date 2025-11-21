package main

import (
    "encoding/json"
    "io"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"

    yaml "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    http.HandleFunc("/api", apiHandler)
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }
    log.Fatal(srv.ListenAndServe())
}

// ...existing code...
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Allow only GET and POST
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Obtain payload either from query param or from request body (limited)
    var payload string
    if p := r.URL.Query().Get("payload"); p != "" {
        payload = p
    } else if r.Method == http.MethodPost {
        const maxBody = 4096 // 4KiB limit
        lr := io.LimitReader(r.Body, maxBody+1)
        b, err := io.ReadAll(lr)
        if err != nil {
            http.Error(w, "invalid request", http.StatusBadRequest)
            return
        }
        if int64(len(b)) > maxBody {
            http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
            return
        }
        payload = string(b)
    }

    payload = strings.TrimSpace(payload)
    if payload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    if len(payload) > 4096 {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Strictly decode only the expected structure to avoid insecure deserialization
    type Payload struct {
        Name string `yaml:"name"`
    }

    var p Payload
    dec := yaml.NewDecoder(strings.NewReader(payload))
    dec.KnownFields(true) // reject unknown fields
    if err := dec.Decode(&p); err != nil {
        http.Error(w, "invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the name field
    p.Name = strings.TrimSpace(p.Name)
    if p.Name == "" {
        http.Error(w, "missing name", http.StatusBadRequest)
        return
    }
    if len(p.Name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Allow only letters, numbers, spaces and a few safe punctuation chars
    validName := regexp.MustCompile(`^[\p{L}\p{N}\s\-\_\.]+$`)
    if !validName.MatchString(p.Name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Return JSON; encoder will escape as needed. Do not leak internal errors.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    resp := map[string]string{"name": p.Name}
    if err := json.NewEncoder(w).Encode(resp); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
}
