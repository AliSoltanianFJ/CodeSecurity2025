package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security controls applied:
    //  - Strict input validation (whitelist: IP or DNS hostname)
    //  - No shell invocation; exec.CommandContext used with arguments only
    //  - Execution timeout to prevent long-running processes
    //  - Truncate output to limit resource exposure
    //  - Secure response headers set

    // Set defensive response headers
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")

    // Read and canonicalize input
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        http.Error(w, "missing 'url' parameter", http.StatusBadRequest)
        return
    }

    // Reject obvious dangerous characters
    if strings.ContainsAny(raw, "&;|$<>`\\'\"") || strings.Contains(raw, "/") {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // If user passed a full URL, extract host
    if strings.Contains(raw, "://") {
        // naive split to avoid importing net/url parsing complexity for this control flow
        parts := strings.SplitN(raw, "://", 2)
        raw = parts[1]
    }
    // strip any path or query fragments if present
    if i := strings.IndexAny(raw, "/?#"); i != -1 {
        raw = raw[:i]
    }
    // strip optional port
    if host, _, err := net.SplitHostPort(raw); err == nil {
        raw = host
    }

    host := strings.ToLower(strings.TrimSpace(raw))
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Validate host: allow IPv4, IPv6, or DNS names per RFC-like constraints
    if !isValidHost(host) {
        http.Error(w, "invalid host format", http.StatusBadRequest)
        return
    }

    // Prepare ping command safely (no shell). Choose args per OS.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    var cmd *exec.Cmd
    if runtime.GOOS == "windows" {
        // Windows: ping -n 1 <host>
        cmd = exec.CommandContext(ctx, "ping", "-n", "1", host)
    } else {
        // Unix-like: ping -c 1 <host>
        cmd = exec.CommandContext(ctx, "ping", "-c", "1", host)
    }

    // Execute command and capture combined output
    out, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        // Do not expose internal command details to clients.
        // Return a generic error and (optionally) minimal diagnostic.
        truncated := truncateOutput(string(out), 1024)
        http.Error(w, fmt.Sprintf("ping failed: %s", truncated), http.StatusBadGateway)
        return
    }

    // Return truncated successful output to avoid huge responses
    w.WriteHeader(http.StatusOK)
    w.Write([]byte(truncateOutput(string(out), 8192)))
}

// isValidHost enforces a strict whitelist: either a valid IP or a valid DNS hostname.
// DNS rules enforced: total length <=253, label length 1..63, labels only [A-Za-z0-9-], no leading/trailing hyphen.
func isValidHost(h string) bool {
    // Quick IP check
    if ip := net.ParseIP(h); ip != nil {
        return true
    }

    // Reject underscores and spaces early
    if strings.ContainsAny(h, " _") {
        return false
    }

    if len(h) > 253 || len(h) == 0 {
        return false
    }

    labels := strings.Split(h, ".")
    labelRegexp := regexp.MustCompile(`^[A-Za-z0-9-]+$`)

    for _, lbl := range labels {
        if lbl == "" || len(lbl) > 63 {
            return false
        }
        if strings.HasPrefix(lbl, "-") || strings.HasSuffix(lbl, "-") {
            return false
        }
        if !labelRegexp.MatchString(lbl) {
            return false
        }
    }
    return true
}

// truncateOutput limits output length (in bytes/characters) and avoids exposing too much.
func truncateOutput(s string, max int) string {
    if len(s) <= max {
        return s
    }
    return s[:max] + "...(truncated)"
}
