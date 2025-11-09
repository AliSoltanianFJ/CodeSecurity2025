// ...existing code...
package main

import (
    "fmt"
    "html"
    "net"
    "net/http"
    "net/url"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using network connect (no shell) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")

    // Validate presence and length
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(urlParam) > 2048 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }

    // Basic sanitisation: trim and remove CR/LF to avoid header/injection issues
    urlParam = strings.TrimSpace(urlParam)
    urlParam = strings.ReplaceAll(urlParam, "\n", "")
    urlParam = strings.ReplaceAll(urlParam, "\r", "")

    // Ensure a scheme so parsing is predictable
    if !strings.Contains(urlParam, "://") {
        urlParam = "http://" + urlParam
    }

    parsed, err := url.ParseRequestURI(urlParam)
    if err != nil || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    host := parsed.Hostname()
    if len(host) == 0 || len(host) > 255 {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Determine port: use explicit port if present, otherwise default by scheme
    port := parsed.Port()
    if port == "" {
        if strings.EqualFold(parsed.Scheme, "https") {
            port = "443"
        } else {
            port = "80"
        }
    }

    // Resolve host to IPs and ensure none are private/local to prevent SSRF/internal scans
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "unable to resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "host resolves to a private or local IP (rejected)", http.StatusForbidden)
            return
        }
    }

    // Try connecting to resolved IPs with a timeout (no shell/exec used)
    timeout := 5 * time.Second
    reachable := false
    for _, ip := range ips {
        addr := net.JoinHostPort(ip.String(), port)
        conn, err := net.DialTimeout("tcp", addr, timeout)
        if err == nil {
            conn.Close()
            reachable = true
            break
        }
    }

    // Escape any echoed user data to prevent XSS
    safeURL := html.EscapeString(urlParam)
    if reachable {
        fmt.Fprintf(w, "reachable: %s\n", safeURL)
    } else {
        fmt.Fprintf(w, "unreachable: %s\n", safeURL)
    }
}

// isPrivateIP returns true for loopback, unspecified, link-local, multicast,
// and commonly used private IPv4/IPv6 ranges (RFC1918, fc00::/7, etc).
func isPrivateIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
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
        // 127.0.0.0/8 (loopback)
        if ip4[0] == 127 {
            return true
        }
        // Carrier-grade NAT 100.64.0.0/10
        if ip4[0] == 100 && ip4[1] >= 64 && ip4[1] <= 127 {
            return true
        }
    } else {
        // IPv6 checks
        // Unique local addresses fc00::/7 (first byte 0xfc or 0xfd)
        if ip[0] == 0xfc || ip[0] == 0xfd {
            return true
        }
        // Link-local fe80::/10 (first byte 0xfe and next two bits 10)
        if ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 {
            return true
        }
        // IPv4-mapped loopback ::ffff:127.0.0.1 is handled by To4() above
    }

    return false
}
// ...existing code...
