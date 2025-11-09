package main

import (
    "fmt"
    "net"
    "net/http"
    "net/url"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using an HTTP HEAD request (secure; no system command). */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse the URL; if no scheme provided, assume http
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Host == "" {
        parsed, err = url.Parse("http://" + urlParam)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Extract hostname for DNS resolution and validation
    host := parsed.Hostname()
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Resolve the hostname to IPs and ensure none are local/private addresses (prevent SSRF)
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "unable to resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "target host is not allowed", http.StatusForbidden)
            return
        }
    }

    // Use a short timeout and perform an HTTP HEAD request instead of calling system ping.
    client := &http.Client{Timeout: 5 * time.Second}
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

// isPrivateIP returns true for loopback, unspecified, link-local and common private IP ranges.
func isPrivateIP(ip net.IP) bool {
    if ip == nil {
        return true
    }
    if ip.IsLoopback() || ip.IsUnspecified() {
        return true
    }
    if ip4 := ip.To4(); ip4 != nil {
        // 10.0.0.0/8
        if ip4[0] == 10 {
            return true
        }
        // 172.16.0.0/12
        if ip4[0] == 172 && ip4[1] >= 16 && ip4[1] <= 31 {
            return true
        }
        // 192.168.0.0/16
        if ip4[0] == 192 && ip4[1] == 168 {
            return true
        }
        // 127.0.0.0/8
        if ip4[0] == 127 {
            return true
        }
        // 169.254.0.0/16 link-local
        if ip4[0] == 169 && ip4[1] == 254 {
            return true
        }
        return false
    }

    // IPv6 checks
    // fc00::/7 unique local addresses
    if len(ip) == net.IPv6len {
        if (ip[0] & 0xfe) == 0xfc {
            return true
        }
        // fe80::/10 link-local
        if ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 {
            return true
        }
        // ::1 loopback handled above by IsLoopback
    }
    return false
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario8CommandInjection\Scenario8.go
package main

import (
    "fmt"
    "net"
    "net/http"
    "net/url"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using an HTTP HEAD request (secure; no system command). */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse the URL; if no scheme provided, assume http
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Host == "" {
        parsed, err = url.Parse("http://" + urlParam)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Extract hostname for DNS resolution and validation
    host := parsed.Hostname()
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Resolve the hostname to IPs and ensure none are local/private addresses (prevent SSRF)
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "unable to resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "target host is not allowed", http.StatusForbidden)
            return
        }
    }

    // Use a short timeout and perform an HTTP HEAD request instead of calling system ping.
    client := &http.Client{Timeout: 5 * time.Second}
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

// isPrivateIP returns true for loopback, unspecified, link-local and common private IP ranges.
func isPrivateIP(ip net.IP) bool {
    if ip == nil {
        return true
    }
    if ip.IsLoopback() || ip.IsUnspecified() {
        return true
    }
    if ip4 := ip.To4(); ip4 != nil {
        // 10.0.0.0/8
        if ip4[0] == 10 {
            return true
        }
        // 172.16.0.0/12
        if ip4[0] == 172 && ip4[1] >= 16 && ip4[1] <= 31 {
            return true
        }
        // 192.168.0.0/16
        if ip4[0] == 192 && ip4[1] == 168 {
            return true
        }
        // 127.0.0.0/8
        if ip4[0] == 127 {
            return true
        }
        // 169.254.0.0/16 link-local
        if ip4[0] == 169 && ip4[1] == 254 {
            return true
        }
        return false
    }

    // IPv6 checks
    // fc00::/7 unique local addresses
    if len(ip) == net.IPv6len {
        if (ip[0] & 0xfe) == 0xfc {
            return true
        }
        // fe80::/10 link-local
        if ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 {
            return true
        }
        // ::1 loopback handled above by IsLoopback
    }
    return false
}
