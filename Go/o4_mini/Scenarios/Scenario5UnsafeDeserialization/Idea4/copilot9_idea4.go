package main

import (
    "fmt"
    "html"
    "net/http"
    "regexp"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

// ...existing code...

var (
    // Whitelist: allow letters, numbers, space, hyphen, underscore, dot
    nameRegexp = regexp.MustCompile(`^[A-Za-z0-9 ._\-]{1,256}$`)
)

func main() {
    http.HandleFunc("/api", apiHandler)

    // Listen with a short ReadHeaderTimeout to mitigate slowloris-like attacks.
    srv := &http.Server{
        Addr:              ":8080",
        ReadHeaderTimeout: 5 * time.Second,
    }
    srv.ListenAndServe()
}

// ...existing code...

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers (defense in depth)
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Only accept GET for this endpoint in this example; reject others.
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Read payload from query parameter (as original). Enforce size limit.
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "payload required", http.StatusBadRequest)
        return
    }
    if len(yamlPayload) > 4096 { // 4 KiB limit; adjust as appropriate
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Define a strict struct to avoid arbitrary type construction during unmarshalling.
    type requestPayload struct {
        Name string `yaml:"name"`
    }

    var req requestPayload
    if err := yaml.Unmarshal([]byte(yamlPayload), &req); err != nil {
        // Return a generic error to avoid leaking parser internals.
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate and canonicalize the "name" field.
    name := strings.TrimSpace(req.Name)
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    if !nameRegexp.MatchString(name) {
        http.Error(w, "invalid name format", http.StatusBadRequest)
        return
    }

    // Output encoding: escape for HTML contexts and return as plain text.
    safeName := html.EscapeString(name)

    // Minimal, safe response.
    fmt.Fprintln(w, safeName)
}

// ...existing code...
