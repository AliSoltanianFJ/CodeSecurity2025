package main

import (
    "encoding/json"
    "html"
    "log"
    "net/http"
    "os"
    "strings"
    "unicode"
    "unicode/utf8"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    // Security decision: require TLS for all requests. Server will refuse to run without certs.
    // Certificates/keys should be provided via environment variables to avoid hardcoding secrets.
    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")
    if certFile == "" || keyFile == "" {
        log.Fatal("TLS_CERT_FILE and TLS_KEY_FILE environment variables must be set; refusing to start insecure HTTP server")
    }

    // Register handler
    http.HandleFunc("/api", apiHandler)

    // Start an HTTP->HTTPS redirector on :8080 to enforce TLS for clients that accidentally use HTTP.
    go func() {
        redirect := func(w http.ResponseWriter, r *http.Request) {
            // Use a fixed, expected HTTPS port and host from request.
            target := "https://" + r.Host + r.URL.RequestURI()
            http.Redirect(w, r, target, http.StatusPermanentRedirect)
        }
        // Note: this redirector intentionally listens on :8080 (insecure) only to redirect.
        // It does not handle any sensitive data.
        if err := http.ListenAndServe(":8080", http.HandlerFunc(redirect)); err != nil {
            log.Printf("HTTP redirector stopped: %v", err)
        }
    }()

    // Start TLS server. This will fail if cert/key are not valid.
    log.Printf("Starting HTTPS server on :8443")
    if err := http.ListenAndServeTLS(":8443", certFile, keyFile, nil); err != nil {
        log.Fatalf("HTTPS server failed: %v", err)
    }
}
// ...existing code...

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security decisions and mitigations:
    // - Require TLS (ensure caller used HTTPS). This avoids sending secrets in cleartext.
    // - Allow only GET for this endpoint (payload is provided as query param per requirements).
    // - Enforce strict input size limits and UTF-8 validation to mitigate DoS/smuggling.
    // - Use yaml.UnmarshalStrict into a fixed struct to avoid arbitrary types and extra fields.
    // - Validate the resulting 'name' field (length, characters) and encode output as JSON.
    // - Return generic error messages and appropriate HTTP status codes (no sensitive details).

    // Enforce TLS usage: reject requests that arrived over plain HTTP (the redirector should be used).
    if r.TLS == nil {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // Only allow GET (query param as specified). Reject other methods.
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Extract payload from query string.
    yamlPayload := r.URL.Query().Get("payload")
    if yamlPayload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Enforce a reasonable size limit for the payload to mitigate resource exhaustion.
    const maxPayloadBytes = 4096
    if len(yamlPayload) > maxPayloadBytes {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Validate UTF-8 and reject unexpected control characters (excluding common whitespace).
    if !utf8.ValidString(yamlPayload) {
        http.Error(w, "invalid payload encoding", http.StatusBadRequest)
        return
    }
    for _, r := range yamlPayload {
        if r == 0 {
            http.Error(w, "invalid payload", http.StatusBadRequest)
            return
        }
        if unicode.IsControl(r) && r != '\n' && r != '\r' && r != '\t' {
            http.Error(w, "invalid payload characters", http.StatusBadRequest)
            return
        }
    }

    // Define a strict schema for expected YAML structure to avoid insecure deserialization.
    var data struct {
        Name string `yaml:"name"`
    }

    // Use yaml.UnmarshalStrict to reject unknown fields and enforce the schema.
    if err := yaml.UnmarshalStrict([]byte(yamlPayload), &data); err != nil {
        // Do not expose internal parsing error details to the client.
        http.Error(w, "invalid payload format", http.StatusBadRequest)
        return
    }

    // Validate the 'name' field according to application policy.
    name := strings.TrimSpace(data.Name)
    if name == "" {
        http.Error(w, "name is required", http.StatusBadRequest)
        return
    }
    const maxNameLen = 256
    if len(name) > maxNameLen {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Restrict characters to a safe subset (letters, numbers, spaces, common punctuation).
    for _, r := range name {
        if !(unicode.IsLetter(r) || unicode.IsDigit(r) || unicode.IsSpace(r) ||
            strings.ContainsRune("._-@'", r)) {
            http.Error(w, "name contains invalid characters", http.StatusBadRequest)
            return
        }
    }

    // Encode output safely. Use JSON response and escape the name to prevent XSS in clients that might
    // render the value without proper context handling.
    safeName := html.EscapeString(name)

    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Minimal response structure
    resp := struct {
        Name string `json:"name"`
    }{Name: safeName}

    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    if err := enc.Encode(resp); err != nil {
        // On encoding failure, return a generic 500 without leaking internal state.
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}
// ...existing code...
