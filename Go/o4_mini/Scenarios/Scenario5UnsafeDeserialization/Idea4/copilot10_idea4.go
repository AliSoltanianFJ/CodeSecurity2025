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
    // Use log.Fatal to ensure server start failures are visible to operators.
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Note: It is safer to accept YAML via a POST body with a strict Content-Type.
    // This handler keeps the original "payload" query param but applies strict limits.

    // 1) Acquire payload (query param)
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // 2) Enforce size limit to mitigate resource exhaustion and long URLs abuse.
    const maxPayloadLen = 4096
    if len(yamlPayload) > maxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // 3) Unmarshal into a fixed, minimal struct to avoid unsafe polymorphic deserialization.
    var input struct {
        Name string `yaml:"name"`
    }
    if err := yaml.Unmarshal([]byte(yamlPayload), &input); err != nil {
        http.Error(w, "invalid yaml payload", http.StatusBadRequest)
        return
    }

    // 4) Canonicalize and validate input using a strict whitelist.
    input.Name = strings.TrimSpace(input.Name)
    if input.Name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }

    // Allow Unicode letters and numbers and a small set of punctuation; max length 256.
    var nameRegex = regexp.MustCompile(`^[\p{L}\p{N} _\-\.,]{1,256}$`)
    if !nameRegex.MatchString(input.Name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // 5) Defense-in-depth HTTP headers.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Security-Policy", "default-src 'none'")

    // Set HSTS only if connection is TLS (do not set over plain HTTP).
    if r.TLS != nil {
        w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    }

    // 6) Escape output to mitigate accidental XSS if this value is embedded in HTML later.
    safeName := html.EscapeString(input.Name)

    // 7) Write response. Avoid logging the raw PII; only log errors.
    if _, err := w.Write([]byte(safeName)); err != nil {
        log.Printf("response write failed: %v", err)
    }
}
// ...existing code...
