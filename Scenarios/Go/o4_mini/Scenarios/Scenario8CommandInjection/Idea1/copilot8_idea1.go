package main

import (
    "fmt"
    "net"
    "net/http"
    urlpkg "net/url"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using a safe HTTP HEAD request (no system command) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Ensure we have a scheme; default to http
    parsed, err := urlpkg.Parse(urlParam)
    if err != nil || parsed.Scheme == "" {
        parsed, err = urlpkg.Parse("http://" + urlParam)
        if err != nil {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Only allow http(s)
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported URL scheme", http.StatusBadRequest)
        return
    }

    host := parsed.Hostname()
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Resolve host and refuse private/loopback addresses to reduce SSRF risk
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "failed to resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "refusing to contact private or loopback address", http.StatusForbidden)
            return
        }
    }

    // HTTP client with timeout and redirect checks (dont follow redirects to private IPs)
    client := &http.Client{
        Timeout: 5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            redirectHost := req.URL.Hostname()
            if redirectHost == "" {
                return http.ErrUseLastResponse
            }
            ips, err := net.LookupIP(redirectHost)
            if err != nil {
                return http.ErrUseLastResponse
            }
            for _, ip := range ips {
                if isPrivateIP(ip) {
                    return http.ErrUseLastResponse
                }
            }
            if len(via) >= 5 {
                return http.ErrUseLastResponse
            }
            return nil
        },
    }

    req, err := http.NewRequest("HEAD", parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusServiceUnavailable)
        return
    }
    defer resp.Body.Close()

    fmt.Fprintf(w, "reachable (status %s)\n", resp.Status)
}

// ...existing code...
func isPrivateIP(ip net.IP) bool {
    privateCIDRs := []string{
        "127.0.0.0/8",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "169.254.0.0/16",
        "::1/128",
        "fc00::/7",
        "fe80::/10",
    }
    for _, cidr := range privateCIDRs {
        _, block, _ := net.ParseCIDR(cidr)
        if block.Contains(ip) {
            return true
        }
    }
    return false
}