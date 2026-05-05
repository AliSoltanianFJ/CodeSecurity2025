package main

import (
    "fmt"
    "net"
    "net/http"
    "net/url"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using an HTTP request (safe alternative to system "ping") */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse and normalize URL. If scheme missing, assume http.
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Host == "" {
        parsed, err = url.Parse("http://" + urlParam)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Only allow http(s) schemes
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported URL scheme", http.StatusBadRequest)
        return
    }

    // Resolve host and block private/loopback addresses to mitigate SSRF
    host := parsed.Hostname()
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "dns lookup failed", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if isPrivateIP(ip) {
            http.Error(w, "refused: target resolves to private or loopback IP", http.StatusBadRequest)
            return
        }
    }

    // Use a short timeout and limited redirects. Prefer HEAD (lighter) and fallback to GET.
    client := &http.Client{
        Timeout: 5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            if len(via) >= 3 {
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
        // Some servers don't support HEAD; try a lightweight GET as fallback.
        req2, err2 := http.NewRequest("GET", parsed.String(), nil)
        if err2 != nil {
            http.Error(w, "failed to create fallback request", http.StatusInternalServerError)
            return
        }
        // ask for minimal body
        req2.Header.Set("Range", "bytes=0-0")
        resp, err = client.Do(req2)
        if err != nil {
            http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusServiceUnavailable)
            return
        }
    }
    defer resp.Body.Close()

    if resp.StatusCode >= 400 {
        http.Error(w, fmt.Sprintf("unreachable (status %s)", resp.Status), http.StatusServiceUnavailable)
        return
    }

    fmt.Fprintf(w, "reachable (status %s)\n", resp.Status)
}

// isPrivateIP returns true for loopback, link-local, private/rfc1918, and unspecified addresses.
func isPrivateIP(ip net.IP) bool {
    if ip == nil {
        return true
    }
    if ip.IsLoopback() || ip.IsUnspecified() {
        return true
    }

    if ip4 := ip.To4(); ip4 != nil {
        switch {
        case ip4[0] == 10:
            return true
        case ip4[0] == 172 && ip4[1] >= 16 && ip4[1] <= 31:
            return true
        case ip4[0] == 192 && ip4[1] == 168:
            return true
        case ip4[0] == 169 && ip4[1] == 254: // link-local
            return true
        case ip4[0] == 127:
            return true
        default:
            return false
        }
    }

    // IPv6: check unique local (fc00::/7), link-local (fe80::/10), loopback ::1
    if len(ip) == net.IPv6len {
        // Unique local: fc00::/7
        if ip[0]&0xfe == 0xfc {
            return true
        }
        // Link-local: fe80::/10
        if ip[0] == 0xfe && (ip[1]&0xc0) == 0x80 {
            return true
        }
        // IPv6 loopback already covered by IsLoopback
    }

    return false
}
// ...existing code...
