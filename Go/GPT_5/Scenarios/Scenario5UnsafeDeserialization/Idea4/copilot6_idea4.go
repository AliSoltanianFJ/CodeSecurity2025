// ...existing code...
package main

import (
    "html"
    "io"
    "log"
    "net/http"
    "os"
    "regexp"
    "time"
    "unicode/utf8"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    // Use a dedicated mux so behaviour is explicit and testable.
    mux := http.NewServeMux()
    mux.HandleFunc("/api", apiHandler)

    // Configure server with safe timeouts and minimal surface area.
    srv := &http.Server{
        Addr:         ":8080", // For production, bind to an internal interface and front with TLS/load-balancer.
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }

    // If TLS cert/key are provided via environment, start TLS; otherwise start plain HTTP (use only for local/testing).
    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")
    if certFile != "" && keyFile != "" {
        log.Println("Starting HTTPS server")
        if err := srv.ListenAndServeTLS(certFile, keyFile); err != nil && err != http.ErrServerClosed {
            log.Fatalf("server failed: %v", err)
        }
        return
    }

    log.Println("Starting HTTP server (use TLS in production)")
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("server failed: %v", err)
    }
}

// deserialize the incoming YAML payload and return the field "name"
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Accept only POST to encourage sending payload in request body.
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Limit the size of incoming payload to prevent resource exhaustion.
    const maxPayloadBytes = 4096
    var reader io.Reader = io.LimitReader(r.Body, maxPayloadBytes+1)

    // Prefer body YAML; fall back to query param if body is empty.
    payloadBytes, err := io.ReadAll(reader)
    if err != nil {
        log.Printf("error reading request body: %v", err)
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    payloadStr := string(payloadBytes)
    if len(payloadStr) == 0 {
        // allow small YAML via query parameter only as fallback (still size-limited)
        payloadStr = r.URL.Query().Get("payload")
        if len(payloadStr) > maxPayloadBytes {
            http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
            return
        }
    }

    if len(payloadStr) == 0 {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }

    // Define strict shape for expected YAML. No generic maps to avoid implicit/unsafe deserialization.
    type payload struct {
        Name string `yaml:"name"`
    }

    var p payload
    dec := yaml.NewDecoder(stringsNewReader(payloadStr))
    // Enforce that only known fields are allowed in input; unknown fields will cause an error.
    dec.KnownFields(true)
    if err := dec.Decode(&p); err != nil {
        // Log a generic error without echoing payload or internal details to the client.
        log.Printf("yaml decode error from %s: %v", r.RemoteAddr, err)
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate the 'name' field strictly:
    // - must be present
    // - reasonable length limits
    // - allow letters, numbers and a small set of safe punctuation
    if p.Name == "" {
        http.Error(w, "missing name", http.StatusBadRequest)
        return
    }
    if utf8.RuneCountInString(p.Name) > 256 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Allow Unicode letters and numbers, spaces, and a small safe set of punctuation.
    var validName = regexp.MustCompile(`^[\p{L}\p{N} .,_'\-]+$`)
    if !validName.MatchString(p.Name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Safe output: use text/plain and escape to avoid XSS if this output is embedded in an HTML context later.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    escaped := html.EscapeString(p.Name)
    if _, err := io.WriteString(w, escaped); err != nil {
        // Log but do not disclose internal details to the client.
        log.Printf("write response error to %s: %v", r.RemoteAddr, err)
    }
}

// Helper wrapper around strings.NewReader to keep imports tidy in this single-file example.
// Using a small wrapper so the import ordering remains clear and explicit.
func stringsNewReader(s string) *stringsReader {
    return &stringsReader{r: s}
}

type stringsReader struct {
    r string
    i int64 // current reading index
}

func (sr *stringsReader) Read(p []byte) (n int, err error) {
    if sr.i >= int64(len(sr.r)) {
        return 0, io.EOF
    }
    n = copy(p, sr.r[sr.i:])
    sr.i += int64(n)
    return n, nil
}
// ...existing code...
