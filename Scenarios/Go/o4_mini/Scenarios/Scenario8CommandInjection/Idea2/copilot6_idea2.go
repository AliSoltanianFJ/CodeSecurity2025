package main

import (
    "context"
    "encoding/json"
    "errors"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "strings"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using HTTP HEAD/GET (no system commands) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    raw := r.URL.Query().Get("url")

    // Basic input validation
    raw = strings.TrimSpace(raw)
    if raw == "" {
        http.Error(w, `{"error":"missing url parameter"}`, http.StatusBadRequest)
        return
    }
    if len(raw) > 2048 {
        http.Error(w, `{"error":"url too long"}`, http.StatusBadRequest)
        return
    }

    // Parse and validate URL
    u, err := url.ParseRequestURI(raw)
    if err != nil || u.Scheme == "" || u.Host == "" {
        http.Error(w, `{"error":"invalid url"}`, http.StatusBadRequest)
        return
    }

    // Allow only http and https schemes
    if u.Scheme != "http" && u.Scheme != "https" {
        http.Error(w, `{"error":"unsupported url scheme"}`, http.StatusBadRequest)
        return
    }

    // Resolve host and ensure it is not a private or loopback address (prevent SSRF)
    host := u.Host
    // strip optional port
    if strings.Contains(host, ":") {
        h, _, err := net.SplitHostPort(host)
        if err == nil {
            host = h
        }
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    ips, err := net.DefaultResolver.LookupIPAddr(ctx, host)
    if err != nil || len(ips) == 0 {
        http.Error(w, `{"error":"host resolution failed"}`, http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isUnallowedIP(ip.IP) {
            http.Error(w, `{"error":"host resolves to disallowed/private address"}`, http.StatusBadRequest)
            return
        }
    }

    // Create HTTP client with timeout and redirect check (prevent redirecting to private IPs)
    client := &http.Client{
        Timeout: 7 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            // disallow too many redirects
            if len(via) >= 10 {
                return errors.New("stopped after too many redirects")
            }
            // validate each redirect target's host
            redirectHost := req.URL.Host
            if strings.Contains(redirectHost, ":") {
                h, _, err := net.SplitHostPort(redirectHost)
                if err == nil {
                    redirectHost = h
                }
            }
            // resolve redirect host
            ctx2, cancel2 := context.WithTimeout(context.Background(), 3*time.Second)
            defer cancel2()
            ips, err := net.DefaultResolver.LookupIPAddr(ctx2, redirectHost)
            if err != nil {
                return errors.New("redirect host resolution failed")
            }
            for _, ip := range ips {
                if isUnallowedIP(ip.IP) {
                    return errors.New("redirect to disallowed/private address")
                }
            }
            return nil
        },
    }

    // Attempt HEAD first (less bandwidth); fallback to GET on 405
    req, err := http.NewRequestWithContext(ctx, http.MethodHead, u.String(), nil)
    if err != nil {
        http.Error(w, `{"error":"failed to create request"}`, http.StatusInternalServerError)
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        // If HEAD fails, try GET (some servers disallow HEAD)
        reqGet, err2 := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
        if err2 != nil {
            http.Error(w, `{"error":"request failed"}`, http.StatusInternalServerError)
            return
        }
        resp, err = client.Do(reqGet)
        if err != nil {
            // Return a safe generic error
            writeJSON(w, map[string]interface{}{
                "url":   u.String(),
                "alive": false,
                "error": "request failed or timed out",
            }, http.StatusOK)
            return
        }
    }
    defer resp.Body.Close()

    alive := resp.StatusCode >= 200 && resp.StatusCode < 400

    writeJSON(w, map[string]interface{}{
        "url":        u.String(),
        "alive":      alive,
        "statusCode": resp.StatusCode,
    }, http.StatusOK)
}

// writeJSON writes a JSON response with safe content-type and minimal exposure
func writeJSON(w http.ResponseWriter, v interface{}, status int) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(v)
}

// isUnallowedIP checks if IP is loopback, unspecified, link-local, or in private ranges.
// Returns true for addresses that should be blocked.
func isUnallowedIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() {
        return true
    }
    // IPv4-mapped IPv6 handling
    ipv4 := ip.To4()
    if ipv4 != nil {
        for _, n := range privateIPv4Nets() {
            if n.Contains(ipv4) {
                return true
            }
        }
        // link-local 169.254.0.0/16
        _, linkLocalNet, _ := net.ParseCIDR("169.254.0.0/16")
        if linkLocalNet.Contains(ipv4) {
            return true
        }
    } else {
        // IPv6 checks: unique local (fc00::/7), link local (fe80::/10)
        _, ulaNet, _ := net.ParseCIDR("fc00::/7")
        _, linkLocal6Net, _ := net.ParseCIDR("fe80::/10")
        if ulaNet.Contains(ip) || linkLocal6Net.Contains(ip) {
            return true
        }
    }
    return false
}

func privateIPv4Nets() []*net.IPNet {
    // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
    nets := []string{
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
    }
    var ipnets []*net.IPNet
    for _, s := range nets {
        _, n, _ := net.ParseCIDR(s)
        ipnets = append(ipnets, n)
    }
    return ipnets
}
