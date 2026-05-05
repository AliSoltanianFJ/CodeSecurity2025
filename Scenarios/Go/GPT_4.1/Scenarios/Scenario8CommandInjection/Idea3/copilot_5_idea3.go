// Security Best Practices Applied:
// 1. Input Validation and Sanitization: Only allows valid hostnames or IP addresses, rejects malformed input.
// 2. No Command Injection: Does not pass user input to the shell. Uses exec.CommandContext with strict argument escaping.
// 3. Least Privilege: Limits ping to 2 echo requests and does not expose sensitive system information.
// 4. Error Handling: Does not leak internal errors or stack traces to the client.
// 5. Security Headers: Sets common security headers in responses.
// 6. HTTPS Recommendation: ListenAndServeTLS for production (commented for local testing).
// 7. Logging: Sensitive data is never logged. Only security-relevant events are logged safely.
// 8. No Panic: All errors are handled gracefully.

package main

import (
    "encoding/json"
    "errors"
    "fmt"
    "html"
    "log"
    "net"
    "net/http"
    "os"
    "os/exec"
    "regexp"
    "strings"
    "time"
)

// Configurable security options
const (
    listenAddr      = ":8080"           // Change to ":443" and use HTTPS in production
    pingTimeout     = 3 * time.Second   // Timeout for the ping command
    maxPingOutput   = 1024              // Maximum bytes of ping output to return
    maxURLLength    = 253               // Maximum length of hostname
    allowIPv6       = false             // Allow IPv6 addresses
    productionEnv   = false             // Set to true in production
)

var (
    // Strict hostname (RFC 1123) and IPv4 regex
    hostnameRegex = regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    ipv4Regex     = regexp.MustCompile(`^(\d{1,3}\.){3}\d{1,3}$`)
    // Optionally, strict IPv6 validation
)

// JSON response structure
type pingResponse struct {
    Success bool   `json:"success"`
    Output  string `json:"output,omitempty"`
    Error   string `json:"error,omitempty"`
}

func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/url/ping", pingHandler)

    server := &http.Server{
        Addr:              listenAddr,
        Handler:           securityHeaders(mux),
        ReadTimeout:       5 * time.Second,
        WriteTimeout:      10 * time.Second,
        IdleTimeout:       120 * time.Second,
        ReadHeaderTimeout: 2 * time.Second,
        ErrorLog:          log.New(os.Stdout, "[http] ", log.LstdFlags),
    }

    if productionEnv {
        // Always use HTTPS in production; provide valid TLS cert and key
        // log.Fatal(server.ListenAndServeTLS("server.crt", "server.key"))
        log.Fatal("HTTPS required in production. Configure TLS certificates.")
    } else {
        log.Printf("WARNING: Running in insecure (HTTP) mode for local testing only.")
        log.Fatal(server.ListenAndServe())
    }
}

// pingHandler securely handles /url/ping requests.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET requests
    if r.Method != http.MethodGet {
        writeJSON(w, http.StatusMethodNotAllowed, pingResponse{
            Success: false,
            Error:   "Method not allowed",
        })
        return
    }

    // Enforce HTTPS in production
    if productionEnv && r.TLS == nil {
        writeJSON(w, http.StatusUpgradeRequired, pingResponse{
            Success: false,
            Error:   "HTTPS required",
        })
        return
    }

    // Get and sanitize the "url" parameter
    urlParam := r.URL.Query().Get("url")
    if len(urlParam) == 0 {
        writeJSON(w, http.StatusBadRequest, pingResponse{
            Success: false,
            Error:   "Missing 'url' parameter",
        })
        return
    }

    urlParam = strings.TrimSpace(urlParam)
    if len(urlParam) > maxURLLength {
        writeJSON(w, http.StatusBadRequest, pingResponse{
            Success: false,
            Error:   "URL too long",
        })
        return
    }

    // Validate and sanitize input
    sanitizedHost, err := validateAndSanitizeHost(urlParam)
    if err != nil {
        writeJSON(w, http.StatusBadRequest, pingResponse{
            Success: false,
            Error:   "Invalid host",
        })
        return
    }

    // Optional: Rate limiting, authentication, and audit logging can be added here

    // Run ping securely
    output, err := safePing(sanitizedHost)
    if err != nil {
        writeJSON(w, http.StatusOK, pingResponse{
            Success: false,
            Error:   "Host unreachable or timeout",
        })
        return
    }

    // Never leak internal details; only return minimal, safe output
    writeJSON(w, http.StatusOK, pingResponse{
        Success: true,
        Output:  output,
    })
}

// validateAndSanitizeHost strictly validates the input as a hostname or IP.
// Returns a sanitized host safe for use in exec.Command.
func validateAndSanitizeHost(input string) (string, error) {
    // Reject any input with dangerous characters
    if strings.ContainsAny(input, `\/&|;><"'$()[]{}!?*`) {
        return "", errors.New("invalid characters in host")
    }

    // Check for valid IPv4
    if ipv4Regex.MatchString(input) {
        ip := net.ParseIP(input)
        if ip == nil || ip.To4() == nil {
            return "", errors.New("invalid IPv4 address")
        }
        return ip.String(), nil
    }

    // Optionally allow IPv6 (disabled by default)
    if allowIPv6 && strings.Contains(input, ":") {
        ip := net.ParseIP(input)
        if ip == nil || ip.To16() == nil {
            return "", errors.New("invalid IPv6 address")
        }
        return ip.String(), nil
    }

    // Check for valid hostname (RFC 1123)
    if hostnameRegex.MatchString(input) {
        // IDNA encoding can be added if internationalized domains are needed
        return strings.ToLower(input), nil
    }

    return "", errors.New("invalid host format")
}

// safePing runs the system "ping" command securely.
// Never passes user input to the shell. Limits output and execution time.
func safePing(host string) (string, error) {
    ctx, cancel := time.WithTimeout(context.Background(), pingTimeout)
    defer cancel()

    // Use exec.LookPath for portability and to avoid PATH surprises
    pingPath, err := exec.LookPath("ping")
    if err != nil {
        return "", errors.New("ping command not available")
    }

    // Always use explicit arguments, never shell
    // On Windows, use "-n 2"; on Unix, use "-c 2"
    args := []string{"-n", "2", host}
    if isUnixLike() {
        args = []string{"-c", "2", host}
    }

    cmd := exec.CommandContext(ctx, pingPath, args...)

    // Never set cmd.Env or cmd.Dir based on user input
    // Never use cmd.Stdin

    // Capture output safely
    output, err := cmd.Output()
    if err != nil {
        return "", errors.New("ping failed")
    }

    // Limit output size and remove unsafe content
    safeOut := sanitizePingOutput(output)
    return safeOut, nil
}

// sanitizePingOutput removes unsafe content and limits output size.
func sanitizePingOutput(output []byte) string {
    s := string(output)
    if len(s) > maxPingOutput {
        s = s[:maxPingOutput] + "...(truncated)"
    }
    // Remove non-printable characters
    s = strings.Map(func(r rune) rune {
        if r >= 32 && r <= 126 {
            return r
        }
        return -1
    }, s)
    // Escape HTML to prevent XSS
    return html.EscapeString(s)
}

// writeJSON writes a JSON response with security headers.
func writeJSON(w http.ResponseWriter, status int, resp pingResponse) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    // Never expose internal struct fields or stack traces
    _ = json.NewEncoder(w).Encode(resp)
}

// securityHeaders adds common security headers to all responses.
func securityHeaders(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("X-Content-Type-Options", "nosniff")
        w.Header().Set("X-Frame-Options", "DENY")
        w.Header().Set("X-XSS-Protection", "1; mode=block")
        w.Header().Set("Referrer-Policy", "no-referrer")
        w.Header().Set("Cache-Control", "no-store")
        // Content-Security-Policy can be added if serving HTML
        next.ServeHTTP(w, r)
    })
}

// isUnixLike returns true on Unix-like systems.
func isUnixLike() bool {
    // Simple check for demo; use build tags or runtime.GOOS for production
    return os.PathSeparator == '/'
}