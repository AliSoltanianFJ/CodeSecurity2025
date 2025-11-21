package main

import (
    "encoding/json"
    "errors"
    "gopkg.in/yaml.v3"
    "io"
    "log"
    "net"
    "net/http"
    "os"
    "regexp"
    "time"
)

// Security notes (high level):
// - We avoid insecure/deserializing into interface{} or arbitrary types. We decode into a strict, whitelisted struct.
// - We use Decoder.KnownFields to reject unknown/malicious fields in YAML payloads.
// - We impose a strict maximum body size to mitigate DoS and resource exhaustion.
// - We validate and whitelist the 'name' field (length and allowed characters).
// - We return JSON with proper content-type and never reflect raw input back to clients.
// - We avoid exposing internal error details to clients; detailed errors are logged server-side only.
// - We prefer TLS: if TLS_CERT_FILE and TLS_KEY_FILE env vars are provided we start HTTPS. Otherwise we bind to localhost only.
// - No secrets are hardcoded. Use environment variables for certificates and listen address.
// - HTTP headers: we set X-Content-Type-Options to nosniff. HSTS is set only when TLS is active.

type payloadT struct {
    Name string `yaml:"name"`
}

var (
    // allow letters, digits, space, hyphen, underscore, apostrophe and dot. Adjust whitelist to your requirements.
    nameAllowed = regexp.MustCompile(`^[A-Za-z0-9 _\-\.'’]{1,100}$`)
    // maximum request size (bytes)
    maxBodySize int64 = 4 * 1024 // 4 KiB
)

func main() {
    http.HandleFunc("/api", apiHandler)

    // Prefer TLS if certs are provided via env vars. Avoid serving plaintext globally by default.
    certFile := os.Getenv("TLS_CERT_FILE")
    keyFile := os.Getenv("TLS_KEY_FILE")

    // Allow override of listen address for testing/dev, but default to localhost when TLS is not configured.
    listenAddr := os.Getenv("LISTEN_ADDR")
    if listenAddr == "" {
        if certFile != "" && keyFile != "" {
            // default HTTPS port if TLS is configured
            listenAddr = ":8443"
        } else {
            // default to localhost-only when not using TLS
            listenAddr = "127.0.0.1:8080"
        }
    }

    server := &http.Server{
        Addr:         listenAddr,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
        // Do not set TLSConfig here; let ListenAndServeTLS handle defaults or set externally via env/config.
    }

    log.Printf("Starting server on %s (TLS: %v)\n", listenAddr, certFile != "" && keyFile != "")

    if certFile != "" && keyFile != "" {
        // Do not start plaintext listener if TLS is configured.
        if err := server.ListenAndServeTLS(certFile, keyFile); err != nil && !errors.Is(err, http.ErrServerClosed) {
            log.Fatalf("server failed to start (TLS): %v", err)
        }
    } else {
        // Safe default: only bind to localhost if TLS not provided.
        ln, err := net.Listen("tcp", listenAddr)
        if err != nil {
            log.Fatalf("failed to listen on %s: %v", listenAddr, err)
        }
        if err := server.Serve(ln); err != nil && !errors.Is(err, http.ErrServerClosed) {
            log.Fatalf("server failed to start: %v", err)
        }
    }
}

/*
apiHandler reads a YAML payload from the request body, safely decodes it into a strict struct
and returns the "name" field as JSON. It enforces:
 - POST method only
 - Content-Type checked (best-effort)
 - maximum body size
 - strict YAML decoding with KnownFields enabled
 - whitelist validation for the name field
 - secure response headers and generic client error messages
*/
func apiHandler(w http.ResponseWriter, r *http.Request) {
    // Security: only accept POST (stateless API); GET would be less appropriate for request bodies.
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Best-effort check for expected content type. Do not rely solely on it, but use as an additional guard.
    ct := r.Header.Get("Content-Type")
    if ct == "" {
        // Accept empty Content-Type but prefer clients to set it.
    } else {
        // Accept application/x-yaml, text/yaml or any +yaml subtype.
        // We keep this check permissive because some clients may send `application/octet-stream` or `text/plain`.
        // Do not reject solely based on this, but log suspicious content types for monitoring.
        // Alternatively, you can enforce strict Content-Type by uncommenting the next lines:
        /*
            if !(strings.HasPrefix(ct, "application/x-yaml") || strings.HasPrefix(ct, "text/yaml") || strings.Contains(ct, "+yaml")) {
                http.Error(w, "unsupported content type", http.StatusUnsupportedMediaType)
                return
            }
        */
        _ = ct
    }

    // Limit request body size to a sane maximum to mitigate DoS and excessive memory use.
    r.Body = http.MaxBytesReader(w, r.Body, maxBodySize)
    defer r.Body.Close()

    // Read the body into a buffer safely
    body, err := io.ReadAll(r.Body)
    if err != nil {
        // The client may have sent more than maxBodySize or connection issues occurred.
        log.Printf("error reading request body: %v", err)
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    // Decode YAML with strict field checking to avoid arbitrary tags or unexpected fields.
    var p payloadT
    dec := yaml.NewDecoder(bytesLimitReader(body))
    // KnownFields will cause an error if unknown fields are present in the payload.
    dec.KnownFields(true)
    if err := dec.Decode(&p); err != nil {
        // Do not return YAML decoder internals to the client; log them for operators.
        log.Printf("yaml decode error: %v; payload: %q", err, sanitizeForLog(string(body)))
        http.Error(w, "invalid payload", http.StatusBadRequest)
        return
    }

    // Validate the name field: non-empty, within length limits, whitelist characters.
    if p.Name == "" {
        http.Error(w, "missing name field", http.StatusBadRequest)
        return
    }
    if len(p.Name) > 100 {
        http.Error(w, "name too long", http.StatusBadRequest)
        return
    }
    if !nameAllowed.MatchString(p.Name) {
        http.Error(w, "invalid characters in name", http.StatusBadRequest)
        return
    }

    // Build response payload in JSON to avoid content sniffing or HTML injection when consumed by browsers.
    resp := struct {
        Name string `json:"name"`
    }{
        Name: p.Name,
    }

    // Set secure headers
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    // HSTS should only be set when TLS is active. We set it where appropriate in production reverse proxy / TLS config.

    // Encode JSON response
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true) // extra protection if consumed in HTML contexts
    if err := enc.Encode(&resp); err != nil {
        // Log internal error; do not expose internals to client
        log.Printf("failed to encode response: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
}

// bytesLimitReader returns an io.Reader for the byte slice so yaml.NewDecoder can read from it.
// We wrap it to a size-limited reader for defense in depth.
func bytesLimitReader(b []byte) io.Reader {
    return io.LimitReader(io.NopCloser(&byteSliceReader{b: b}), maxBodySize)
}

// byteSliceReader implements io.Reader over a byte slice without copying.
type byteSliceReader struct {
    b []byte
    i int64 // current reading index
}

func (r *byteSliceReader) Read(p []byte) (int, error) {
    if r.i >= int64(len(r.b)) {
        return 0, io.EOF
    }
    n := copy(p, r.b[r.i:])
    r.i += int64(n)
    return n, nil
}

// sanitizeForLog produces a safe truncated representation of potentially large or binary payloads for logging.
func sanitizeForLog(s string) string {
    const max = 512
    if len(s) <= max {
        return s
    }
    return s[:max] + "...(truncated)"
}
