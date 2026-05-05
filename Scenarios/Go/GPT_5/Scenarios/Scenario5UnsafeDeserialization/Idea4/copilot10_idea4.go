package main

import (
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    "gopkg.in/yaml.v3"
)

// Secure HTTP server configuration and YAML parsing handler.
// Security decisions documented inline:
// - Do NOT perform generic/untyped YAML deserialization. Instead parse into a yaml.Node
//   and only extract the allowed scalar "name" field. This prevents arbitrary type instantiation
//   and unsafe deserialization (RCE, gadget chains).
// - Enforce TLS by default. Plain HTTP is only allowed if ALLOW_INSECURE=1 is set
//   (explicit developer override for local testing).
// - Limit payload size and name length to mitigate DoS and resource exhaustion.
// - Validate allowed characters in the name using a conservative Unicode-aware regexp.
// - Return JSON responses and avoid leaking internal error details to clients.
// - Use server timeouts to reduce exposure to slowloris-style attacks.

func main() {
    http.HandleFunc("/api", apiHandler)

    // Use timeouts for safer defaults.
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
        Handler:      nil, // default mux
    }

    // If TLS cert/key are provided via environment variables, start TLS.
    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")
    if certFile != "" && keyFile != "" {
        log.Printf("Starting HTTPS server on %s", srv.Addr)
        if err := srv.ListenAndServeTLS(certFile, keyFile); err != nil && err != http.ErrServerClosed {
            log.Fatalf("server error: %v", err)
        }
        return
    }

    // No certs provided. Start plain HTTP only if explicitly allowed (development).
    if os.Getenv("ALLOW_INSECURE") == "1" {
        log.Printf("Starting HTTP server on %s (INSECURE: ALLOW_INSECURE=1)", srv.Addr)
        if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            log.Fatalf("server error: %v", err)
        }
        return
    }

    log.Fatalf("TLS certificate/key not provided. Set TLS_CERT_FILE and TLS_KEY_FILE to enable HTTPS, or set ALLOW_INSECURE=1 to run without TLS (not recommended).")
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Enforce TLS unless explicitly allowed (see main).
    if r.TLS == nil && os.Getenv("ALLOW_INSECURE") != "1" {
        http.Error(w, "HTTPS is required", http.StatusUpgradeRequired) // 426
        return
    }

    // Only allow GET and POST for this endpoint; reject others.
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Retrieve payload from query param "payload" (per requirement).
    // Limit the acceptable length to mitigate resource exhaustion via very large queries.
    const maxPayloadLen = 2048
    payload := r.URL.Query().Get("payload")
    if payload == "" {
        http.Error(w, "missing payload", http.StatusBadRequest)
        return
    }
    if len(payload) > maxPayloadLen {
        http.Error(w, "payload too large", http.StatusRequestEntityTooLarge)
        return
    }

    // Trim whitespace. Work with a copy to avoid modifying shared memory.
    payload = strings.TrimSpace(payload)
    if payload == "" {
        http.Error(w, "empty payload", http.StatusBadRequest)
        return
    }

    // Parse YAML into a yaml.Node (structured parse) rather than into interface{} or any typed struct
    // to avoid invoking type-specific unmarshalling that could be abused.
    var doc yaml.Node
    dec := yaml.NewDecoder(strings.NewReader(payload))
    // Prevent YAML decoder from decoding into native Go types automatically; we manually inspect nodes.
    if err := dec.Decode(&doc); err != nil {
        // Do not expose YAML parse errors to the client to avoid leaking internal details.
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate YAML structure: expect a document with a top-level mapping node.
    if len(doc.Content) == 0 {
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }
    root := doc.Content[0]
    if root.Kind != yaml.MappingNode {
        http.Error(w, "expected a mapping with a single 'name' key", http.StatusBadRequest)
        return
    }

    // Extract keys. YAML mapping node content is a sequence of key,value,key,value...
    var nameVal *yaml.Node
    for i := 0; i < len(root.Content); i += 2 {
        keyNode := root.Content[i]
        valNode := root.Content[i+1]

        // Keys must be simple scalars.
        if keyNode.Kind != yaml.ScalarNode {
            http.Error(w, "invalid payload key", http.StatusBadRequest)
            return
        }
        key := keyNode.Value
        if key != "name" {
            // Reject any unexpected top-level keys to be strict and avoid future surprises.
            http.Error(w, "unexpected field present", http.StatusBadRequest)
            return
        }
        // Found 'name'
        nameVal = valNode
    }

    if nameVal == nil {
        http.Error(w, "'name' field required", http.StatusBadRequest)
        return
    }

    // The 'name' value must be a plain scalar (string). Reject complex nodes (sequences, mappings, anchors, tags).
    if nameVal.Kind != yaml.ScalarNode {
        http.Error(w, "'name' must be a scalar string", http.StatusBadRequest)
        return
    }

    // Prevent multiline values to reduce vector for injection-like content embedded in payload.
    if strings.ContainsAny(nameVal.Value, "\n\r") {
        http.Error(w, "invalid name value", http.StatusBadRequest)
        return
    }

    // Validate allowed characters and length. Use Unicode-aware character classes.
    const maxNameLen = 256
    name := strings.TrimSpace(nameVal.Value)
    if name == "" || len(name) > maxNameLen {
        http.Error(w, "invalid name length", http.StatusBadRequest)
        return
    }

    // Accept letters, numbers, spaces and a small set of punctuation commonly used in names.
    // This reduces risk of script injection or malicious payloads.
    var validName = regexp.MustCompile(`^[\p{L}\p{N}\s._-]{1,256}$`)
    if !validName.MatchString(name) {
        http.Error(w, "invalid name characters", http.StatusBadRequest)
        return
    }

    // Return JSON response. Use json.Encoder which safely escapes data.
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    resp := map[string]string{"name": name}
    if err := json.NewEncoder(w).Encode(resp); err != nil {
        // Log the internal error for operators, but return a generic message to the client.
        log.Printf("could not encode response: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}

// NOTE: helper function for explicit error wrapping (keeps internal errors internal).
func wrapInternal(err error) error {
    if err == nil {
        return nil
    }
    return errors.New("internal server error")
}
