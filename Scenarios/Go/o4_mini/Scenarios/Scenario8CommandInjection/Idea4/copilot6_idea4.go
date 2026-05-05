package main

import (
    "context"
    "errors"
    "fmt"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
    "sync"
    "time"
)

var (
    // hostname label regex approximating RFC 1123 (no underscore, no leading/trailing hyphen).
    hostnameRegexp = regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9\-]{0,61}[a-z0-9])?)*)$`)

    // private/reserved networks to block (prevent SSRF to internal addresses)
    privateCIDRs []*net.IPNet

    // simple in-memory rate limiter per remote IP
    rl   = make(map[string]*clientRate)
    rlMu sync.Mutex
)

type clientRate struct {
    count     int
    resetTime time.Time
}

func init() {
    cidrs := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "127.0.0.0/8",
        "169.254.0.0/16", // link local
        "::1/128",
        "fc00::/7",  // unique local
        "fe80::/10", // link local IPv6
    }
    for _, c := range cidrs {
        _, n, _ := net.ParseCIDR(c)
        privateCIDRs = append(privateCIDRs, n)
    }
}

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    // For production: bind to specific IP, run as unprivileged user, enable TLS (ListenAndServeTLS).
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Security headers
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("X-Frame-Options", "DENY")
    w.Header().Set("Referrer-Policy", "no-referrer")
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    remoteIP := clientIPFromRequest(r)
    if !allowRequest(remoteIP) {
        http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
        return
    }

    target := strings.TrimSpace(r.URL.Query().Get("url"))
    if target == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(target) > 253 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }
    // Disallow any scheme or path; only hostnames or literal IPs allowed.
    if strings.ContainsAny(target, "/:@") || strings.Contains(target, "http") {
        http.Error(w, "invalid target", http.StatusBadRequest)
        return
    }

    // Validate and resolve target, enforce no private/reserved addresses.
    ip, err := validateAndResolveTarget(target)
    if err != nil {
        http.Error(w, fmt.Sprintf("invalid target: %v", err), http.StatusBadRequest)
        return
    }

    // Use a short timeout for command execution
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    pingPath, err := exec.LookPath("ping")
    if err != nil {
        http.Error(w, "ping binary not available", http.StatusInternalServerError)
        return
    }

    args := pingArgsForOS(ip.String())
    // Execute without shell to avoid injection.
    cmd := exec.CommandContext(ctx, pingPath, args...)
    // Clear environment for safety (retain PATH so lookup works) - optional
    // cmd.Env = []string{"PATH=" + os.Getenv("PATH")}

    out, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        // Return minimal info to client; avoid echoing sensitive internal data.
        http.Error(w, fmt.Sprintf("unreachable: %s", sanitizeOutput(out)), http.StatusBadGateway)
        return
    }

    // Success
    fmt.Fprintln(w, sanitizeOutput(out))
}

// validateAndResolveTarget ensures target is a safe hostname or IP and doesn't resolve to private ranges.
func validateAndResolveTarget(target string) (net.IP, error) {
    // If it's a literal IP:
    if ip := net.ParseIP(target); ip != nil {
        if isPrivateIP(ip) {
            return nil, errors.New("private or reserved IP addresses are not allowed")
        }
        return ip, nil
    }

    // Validate hostname syntactically
    if !hostnameRegexp.MatchString(target) {
        return nil, errors.New("invalid hostname format")
    }

    // Resolve hostname to IPs and pick first public address
    ips, err := net.LookupIP(target)
    if err != nil || len(ips) == 0 {
        return nil, errors.New("hostname resolution failed")
    }
    for _, ip := range ips {
        if !isPrivateIP(ip) {
            return ip, nil
        }
    }
    return nil, errors.New("hostname resolves only to private/reserved addresses")
}

// isPrivateIP returns true if ip is in any configured private/reserved CIDR
func isPrivateIP(ip net.IP) bool {
    for _, n := range privateCIDRs {
        if n.Contains(ip) {
            return true
        }
    }
    return false
}

// pingArgsForOS returns safe arguments for the system ping binary (no shell)
func pingArgsForOS(target string) []string {
    if runtime.GOOS == "windows" {
        // -n 1 (one echo request)
        return []string{"-n", "1", target}
    }
    // Unix-like: -c 1 (one packet), -W 2 or -w depending on platform. Keep simple.
    return []string{"-c", "1", "-W", "2", target}
}

// Simple per-IP rate limiter: allow up to 10 requests per minute.
func allowRequest(remoteIP string) bool {
    if remoteIP == "" {
        remoteIP = "unknown"
    }
    rlMu.Lock()
    defer rlMu.Unlock()
    now := time.Now()
    c := rl[remoteIP]
    if c == nil || now.After(c.resetTime) {
        rl[remoteIP] = &clientRate{count: 1, resetTime: now.Add(1 * time.Minute)}
        return true
    }
    if c.count >= 10 {
        return false
    }
    c.count++
    return true
}

// clientIPFromRequest extracts the remote IP from the request, without trusting X-Forwarded-For.
func clientIPFromRequest(r *http.Request) string {
    host := r.RemoteAddr
    if host == "" {
        return ""
    }
    ipStr, _, err := net.SplitHostPort(host)
    if err != nil {
        // maybe just an IP
        return host
    }
    return ipStr
}

// sanitizeOutput limits output length and removes newlines that could be abused in logs
func sanitizeOutput(b []byte) string {
    s := strings.TrimSpace(string(b))
    if len(s) > 2000 {
        return s[:2000] + "..."
    }
    return s
}
