// ...existing code...
package main

import (
    "bytes"
    "encoding/json"
    "io"
    "log"
    "net/http"
    "os"
    "strings"
    "time"
    "unicode"
    "unicode/utf8"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    // Use a ServeMux so handlers can be extended safely later.
    mux := http.NewServeMux()
    mux.HandleFunc("/api", apiHandler)

    // Secure defaults: require TLS certificates via environment variables.
    cert := os.Getenv("TLS_CERT_FILE")
    key := os.Getenv("TLS_KEY_FILE")
    if cert == "" || key == "" {
        // Fail fast: do not run an HTTP server without TLS in production.
        log.Fatal("TLS_CERT_FILE and TLS_KEY_FILE environment variables must be set to enable HTTPS; aborting startup")
    }

    srv := &http.Server{
        Addr:         ":8443", // secure default HTTPS port for this service
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }

    log.Printf("Starting HTTPS server on %s", srv.Addr)
    log.Fatal(srv.ListenAndServeTLS(cert, key))
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions:
    // - Only accept POST requests.
    // - Use a strict YAML decoder (yaml.v3) with KnownFields(true) to prevent
    //   unexpected fields or type coercion.
    // - Limit request payload size and validate UTF-8 and character ranges.
    // - Do not leak internal error details to clients; log server-side only.
    // - Return JSON with proper encoding to avoid XSS when consumed in browsers.

    if r.Method != http.MethodPost {
        w.Header().Set("Allow", http.MethodPost)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit the size of the request body (protect against large payloads).
    const maxPayloadSize = 4096 // bytes
    r.Body = http.MaxBytesReader(w, r.Body, maxPayloadSize)
    defer r.Body.Close()

    // Read the body
    body, err := io.ReadAll(r.Body)
    if err != nil {
        log.Printf("error reading request body: %v", err)
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    // If no body was provided, allow a small fallback to a 'payload' query param.
    // Prefer body for YAML input; query param is less ideal but supported for compatibility.
    if len(body) == 0 {
        q := strings.TrimSpace(r.URL.Query().Get("payload"))
        if q == "" {
            http.Error(w, "missing payload", http.StatusBadRequest)
            return
        }
        body = []byte(q)
        if len(body) > maxPayloadSize {
            http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
            return
        }
    }

    // Basic validation: must be valid UTF-8 and not contain NUL bytes.
    if !utf8.Valid(body) || bytes.IndexByte(body, 0) != -1 {
        http.Error(w, "invalid payload encoding", http.StatusBadRequest)
        return
    }

    // Decode YAML into a strict, minimal struct to prevent unsafe deserialization.
    var payload struct {
        Name string `yaml:"name"`
    }

    dec := yaml.NewDecoder(bytes.NewReader(body))
    dec.KnownFields(true) // reject unknown fields for strictness

    if err := dec.Decode(&payload); err != nil {
        // Log detailed error server-side for audit, return generic error to client.
        log.Printf("yaml decode error: %v", err)
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate the 'name' field according to expected constraints.
    name := strings.TrimSpace(payload.Name)
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    if len(name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Reject control characters in the name to avoid injection/XSS risks when rendered.
    for _, r := range name {
        if unicode.IsControl(r) {
            http.Error(w, "invalid characters in name", http.StatusBadRequest)
            return
        }
    }

    // Return the result as JSON with proper encoding.
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    resp := struct {
        Name string `json:"name"`
    }{
        Name: name,
    }

    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        log.Printf("failed to write response: %v", err)
        // Don't expose internal error details.
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}