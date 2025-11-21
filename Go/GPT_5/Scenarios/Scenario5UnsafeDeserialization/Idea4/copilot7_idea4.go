package main

import (
    "encoding/json"
    "io"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"
    "unicode/utf8"

    "gopkg.in/yaml.v3"
)

// ...existing code...
func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/api", apiHandler)

    // Secure server configuration with sensible timeouts and TLS required.
    srv := &http.Server{
        Addr:         ":8443", // secure default - requires TLS in production
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
    }

    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")
    if certFile == "" || keyFile == "" {
        log.Fatal("TLS_CERT_FILE and TLS_KEY_FILE must be set. Refusing to start without TLS for security.")
    }

    log.Printf("Starting HTTPS server on %s", srv.Addr)
    log.Fatal(srv.ListenAndServeTLS(certFile, keyFile))
}

// ...existing code...
/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce POST to minimize accidental information disclosure via GET
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Require TLS for this endpoint (server is configured to use TLS)
    if r.TLS == nil {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // Strict Content-Type validation to avoid ambiguous parsing
    ct := r.Header.Get("Content-Type")
    if ct == "" {
        http.Error(w, "Content-Type required", http.StatusBadRequest)
        return
    }
    allowedCT := []string{"application/x-yaml", "application/yaml", "text/yaml"}
    okCT := false
    for _, a := range allowedCT {
        if strings.HasPrefix(ct, a) {
            okCT = true
            break
        }
    }
    if !okCT {
        http.Error(w, "Unsupported Content-Type", http.StatusUnsupportedMediaType)
        return
    }

    // Limit body size to protect against large payload attacks
    const maxBodySize = 4096 // 4 KiB - adjust as needed
    r.Body = http.MaxBytesReader(w, r.Body, maxBodySize)
    body, err := io.ReadAll(r.Body)
    if err != nil {
        log.Printf("failed to read request body: %v", err)
        http.Error(w, "Invalid request body", http.StatusBadRequest)
        return
    }

    // Whitelisted struct to avoid unsafe deserialization and RCE
    type Payload struct {
        Name string `yaml:"name"`
    }
    var p Payload

    // Use strict unmarshalling to reject unknown fields and reduce attack surface
    if err := yaml.UnmarshalStrict(body, &p); err != nil {
        log.Printf("yaml unmarshal error: %v", err)
        http.Error(w, "Invalid YAML payload", http.StatusBadRequest)
        return
    }

    // Validate and sanitize the 'name' field
    name := strings.TrimSpace(p.Name)
    if name == "" {
        http.Error(w, "name field is required", http.StatusBadRequest)
        return
    }
    if !utf8.ValidString(name) {
        http.Error(w, "invalid name encoding", http.StatusBadRequest)
        return
    }
    if len(name) > 200 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    // Allow letters, numbers, spaces and a small set of punctuation; reject markup/control characters
    validName := regexp.MustCompile(`^[\p{L}\p{N}\s\-\_\.]+$`)
    if !validName.MatchString(name) {
        http.Error(w, "name contains invalid characters", http.StatusBadRequest)
        return
    }

    // Safe JSON response encoding to prevent XSS when consumed by browsers
    resp := map[string]string{"name": name}
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Security headers
    w.Header().Set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")

    if err := json.NewEncoder(w).Encode(resp); err != nil {
        log.Printf("failed to encode response: %v", err)
        // Do not leak internal errors to the client
    }
}
