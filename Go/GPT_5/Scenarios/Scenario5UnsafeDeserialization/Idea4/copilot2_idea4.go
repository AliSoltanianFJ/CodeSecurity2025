// ...existing code...
package main

import (
    "errors"
    "html"
    "io"
    "log"
    "net/http"
    "regexp"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    // Security decision: the server enforces TLS at the handler level (see apiHandler).
    // In production, run this service behind a proper TLS-terminating reverse proxy or
    // use http.ListenAndServeTLS with certificates managed by a secure process.
    http.HandleFunc("/api", apiHandler)

    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
        // Principle of least privilege: no extra timeouts or handlers that increase surface.
    }
    log.Println("starting server on :8080 (ensure TLS at proxy in production)")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions (documented):
    // - Require TLS (r.TLS != nil) to protect data in transit. In many deployments TLS is terminated
    //   by a reverse proxy; this handler will require it and reject plain HTTP to avoid accidental leak.
    // - Limit accepted payload size to avoid DoS from large bodies.
    // - Unmarshal YAML into a strict struct (no interface{} or map[string]interface{}) to avoid
    //   unsafe/insecure deserialization of arbitrary types.
    // - Validate and sanitize the 'name' field thoroughly before echoing it back.
    // - Log minimal info and never include raw payloads or secrets in logs or error responses.

    // Enforce TLS usage to protect sensitive data in transit.
    if r.TLS == nil {
        // Don't leak configuration details to the client; instruct to use TLS.
        http.Error(w, "TLS required; please use HTTPS", http.StatusUpgradeRequired)
        log.Printf("rejected non-TLS request from %s", r.RemoteAddr)
        return
    }

    // Accept payload either via query parameter "payload" (small) or via POST body.
    const maxQueryLen = 4096        // reasonable upper bound for query-based YAML
    const maxBodyBytes = int64(8192) // limit body to 8 KB

    yamlPayload := strings.TrimSpace(r.URL.Query().Get("payload"))
    var raw string
    if yamlPayload != "" {
        if len(yamlPayload) > maxQueryLen {
            http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
            log.Printf("rejected oversized query payload from %s", r.RemoteAddr)
            return
        }
        raw = yamlPayload
    } else {
        // Only allow POST for body-based payloads.
        if r.Method != http.MethodPost {
            http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
            return
        }
        // Limit body size to avoid resource exhaustion.
        r.Body = http.MaxBytesReader(w, r.Body, maxBodyBytes)
        body, err := io.ReadAll(r.Body)
        if err != nil {
            http.Error(w, "unable to read payload", http.StatusBadRequest)
            log.Printf("error reading body from %s: %v", r.RemoteAddr, err)
            return
        }
        raw = strings.TrimSpace(string(body))
        if raw == "" {
            http.Error(w, "empty payload", http.StatusBadRequest)
            return
        }
    }

    // Define a strict struct for the expected YAML shape.
    type payloadStruct struct {
        Name string `yaml:"name"`
    }

    var pl payloadStruct
    if err := yaml.Unmarshal([]byte(raw), &pl); err != nil {
        // Avoid returning parser internals to the client.
        http.Error(w, "invalid YAML payload", http.StatusBadRequest)
        log.Printf("yaml unmarshal error from %s: %v", r.RemoteAddr, err)
        return
    }

    // Validate the 'name' field: required, trimmed, reasonable length, and restricted charset.
    name := strings.TrimSpace(pl.Name)
    if name == "" {
        http.Error(w, "missing required field 'name'", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "field 'name' too long", http.StatusBadRequest)
        return
    }

    // Restrict characters to a safe set (Unicode letters/numbers and a few punctuation).
    // This avoids injection and XSS risks when echoing back the value.
    var validName = regexp.MustCompile(`^[\p{L}\p{N} _\-\.\']+$`)
    if !validName.MatchString(name) {
        http.Error(w, "field 'name' contains invalid characters", http.StatusBadRequest)
        return
    }

    // Final sanitization: escape for safe inclusion in an HTTP response body.
    escaped := html.EscapeString(name)

    // Return as plain text with an explicit Content-Type and minimal information.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if _, err := w.Write([]byte(escaped)); err != nil {
        // On write failure, log server-side; do not reveal internals to client.
        log.Printf("failed to write response to %s: %v", r.RemoteAddr, err)
    }
}

// ...existing code...
