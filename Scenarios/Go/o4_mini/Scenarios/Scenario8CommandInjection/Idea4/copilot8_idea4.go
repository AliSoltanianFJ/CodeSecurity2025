package main

import (
    "context"
    "encoding/json"
    "fmt"
    "log"
    "net"
    "net/http"
    "net/url"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// pingHandler checks whether a URL is reachable without invoking shell commands.
// It validates the input, prevents SSRF by blocking private/reserved IPs, and
// uses timeouts to limit resource usage. No exec.Command is used.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Response headers - security hardening
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.Header().Set("Cache-Control", "no-store")

    // Strict input retrieval and basic limits
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        writeJSON(w, http.StatusBadRequest, map[string]any{"error": "missing url parameter"})
        return
    }
    if len(raw) > 2048 {
        writeJSON(w, http.StatusBadRequest, map[string]any{"error": "url too long"})
        return
    }

    // Parse and validate URL
    parsed, err := url.Parse(raw)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        writeJSON(w, http.StatusBadRequest, map[string]any{"error": "invalid url"})
        return
    }
    // Enforce allowed schemes only
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        writeJSON(w, http.StatusBadRequest, map[string]any{"error": "only http/https schemes are allowed"})
        return
    }
    // Disallow embedded credentials (userinfo)
    if parsed.User != nil {
        writeJSON(w, http.StatusBadRequest, map[string]any{"error": "credentials in url are not allowed"})
        return
    }

    // Extract hostname for DNS resolution and SSRF checks
    host := parsed.Hostname()
    if host == "" {
        writeJSON(w, http.StatusBadRequest, map[string]any{"error": "invalid host"})
        return
    }

    // Resolve host to IPs and block private/loopback addresses
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        writeJSON(w, http.StatusBadGateway, map[string]any{"error": "host resolution failed"})
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            writeJSON(w, http.StatusForbidden, map[string]any{"error": "access to private/internal addresses is forbidden"})
            return
        }
    }

    // Perform a HEAD request with conservative timeouts. Fall back to GET only if HEAD not allowed.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        writeJSON(w, http.StatusInternalServerError, map[string]any{"error": "failed to create request"})
        return
    }

    client := &http.Client{
        Timeout: 5 * time.Second,
        Transport: &http.Transport{
            Proxy:               http.ProxyFromEnvironment,
            DisableKeepAlives:   true,
            TLSHandshakeTimeout: 3 * time.Second,
            DialContext: (&net.Dialer{
                Timeout:   3 * time.Second,
                KeepAlive: 0,
            }).DialContext,
        },
    }

    start := time.Now()
    resp, err := client.Do(req)
    elapsed := time.Since(start)

    // If HEAD is not allowed (405) or returns no useful info, try GET safely.
    if err != nil {
        // try GET as fallback
        reqGet, rerr := http.NewRequestWithContext(ctx, http.MethodGet, parsed.String(), nil)
        if rerr == nil {
            start = time.Now()
            resp, err = client.Do(reqGet)
            elapsed = time.Since(start)
        }
    }
    if err != nil {
        // Consider this a failure to reach the host
        writeJSON(w, http.StatusBadGateway, map[string]any{
            "alive":    false,
            "duration": elapsed.Milliseconds(),
            "error":    fmt.Sprintf("request error: %v", err),
        })
        return
    }
    defer resp.Body.Close()

    // Successful reachability check
    writeJSON(w, http.StatusOK, map[string]any{
        "alive":       resp.StatusCode >= 200 && resp.StatusCode < 400,
        "status_code": resp.StatusCode,
        "duration_ms": elapsed.Milliseconds(),
        "headers":     map[string]any{"server": resp.Header.Get("Server")},
    })
}

// writeJSON helper to marshal and write JSON responses
func writeJSON(w http.ResponseWriter, status int, payload any) {
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(payload)
}

// isPrivateIP returns true for loopback, unspecified, link-local, multicast, and RFC1918 / unique-local addresses
func isPrivateIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() {
        return true
    }
    // IPv4 checks
    if ipv4 := ip.To4(); ipv4 != nil {
        b0 := ipv4[0]
        b1 := ipv4[1]
        switch {
        case b0 == 10:
            return true // 10.0.0.0/8
        case b0 == 127:
            return true // 127.0.0.0/8
        case b0 == 169 && b1 == 254:
            return true // 169.254.0.0/16 link-local
        case b0 == 172 && (b1 >= 16 && b1 <= 31):
            return true // 172.16.0.0/12
        case b0 == 192 && b1 == 168:
            return true // 192.168.0.0/16
        }
        return false
    }

    // IPv6 checks
    if ip.To16() != nil {
        // fc00::/7 unique local, fe80::/10 link-local, ::1 loopback handled earlier
        if ip[0]&0xfe == 0xfc { // fc00::/7
            return true
        }
        if ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 { // fe80::/10
            return true
        }
        // multicast ff00::/8
        if ip[0] == 0xff {
            return true
        }
    }
    return false
}
