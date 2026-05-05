package main

import (
    "fmt"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    // Use an http.Server with conservative timeouts to reduce impact of slowloris/DoS.
    srv := &http.Server{
        Addr:         ":8080",
        Handler:      nil, // default mux
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
        IdleTimeout:  30 * time.Second,
        // In production, run behind TLS (HTTP/2) and load balancer; do not expose raw HTTP.
    }
    http.HandleFunc("/api", apiHandler)
    log.Println("starting server on :8080")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce allowed method (query-based API only accepts GET here to match existing usage).
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read payload from query param (as pre-existing contract). Limit size to mitigate DoS.
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    if len(payload) > 2048 { // conservative upper bound
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Canonicalize payload line endings to reduce ambiguity.
    payload = strings.ReplaceAll(payload, "\r\n", "\n")

    // Define strict schema for YAML to avoid unsafe, arbitrary types during decode.
    type requestPayload struct {
        Name string `yaml:"name"`
    }

    var p requestPayload

    // Use yaml.Decoder with KnownFields(true) to reject unknown fields.
    dec := yaml.NewDecoder(strings.NewReader(payload))
    dec.KnownFields(true)
    if err := dec.Decode(&p); err != nil {
        // Do not return detailed parse errors to client to avoid information leakage.
        log.Printf("yaml decode error: %v", err)
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Whitelist validation for the 'name' field: allow letters, digits, space, -, _, .
    // Enforce reasonable length to avoid abuse when echoed back.
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name required", http.StatusUnprocessableEntity)
        return
    }
    if len(name) > 100 {
        http.Error(w, "name too long", http.StatusRequestEntityTooLarge)
        return
    }
    var validName = regexp.MustCompile(`^[A-Za-z0-9 ._\-]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid name value", http.StatusUnprocessableEntity)
        return
    }

    // Output as text/plain to avoid accidental HTML rendering; avoid logging PII.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    _, _ = fmt.Fprintln(w, name)
}
