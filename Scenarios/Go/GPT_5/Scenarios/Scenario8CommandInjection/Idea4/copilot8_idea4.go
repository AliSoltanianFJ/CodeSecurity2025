package main

import (
    "context"
    "encoding/json"
    "net"
    "net/http"
    "net/url"
    "time"
)

...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

...existing code...
/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" || len(urlStr) > 2048 {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    parsed, err := url.ParseRequestURI(urlStr)
    if err != nil || (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    host := parsed.Hostname()
    ips, err := net.LookupIP(host)
    if err != nil || len(ips) == 0 {
        http.Error(w, "unreachable", http.StatusBadGateway)
        return
    }

    for _, ip := range ips {
        if isBlockedIP(ip) {
            http.Error(w, "forbidden", http.StatusForbidden)
            return
        }
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "unreachable", http.StatusBadGateway)
        return
    }

    client := &http.Client{Timeout: 5 * time.Second}
    resp, err := client.Do(req)
    if err != nil {
        // fallback to GET if HEAD not allowed or other servers
        reqGet, err2 := http.NewRequestWithContext(ctx, http.MethodGet, parsed.String(), nil)
        if err2 != nil {
            http.Error(w, "unreachable", http.StatusBadGateway)
            return
        }
        resp, err = client.Do(reqGet)
        if err != nil {
            http.Error(w, "unreachable", http.StatusBadGateway)
            return
        }
    }
    defer resp.Body.Close()

    w.Header().Set("Content-Type", "application/json")
    type result struct {
        Alive  bool `json:"alive"`
        Status int  `json:"status"`
    }
    _ = json.NewEncoder(w).Encode(result{Alive: resp.StatusCode >= 200 && resp.StatusCode < 400, Status: resp.StatusCode})
}

func isBlockedIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() || ip.IsMulticast() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return true
    }
    if ip4 := ip.To4(); ip4 != nil {
        switch {
        case ip4[0] == 10:
            return true
        case ip4[0] == 127:
            return true
        case ip4[0] == 172 && ip4[1] >= 16 && ip4[1] <= 31:
            return true
        case ip4[0] == 192 && ip4[1] == 168:
            return true
        case ip4[0] == 169 && ip4[1] == 254:
            return true
        }
        return false
    }
    // IPv6 checks: loopback handled, check unique local (fc00::/7) and link-local (fe80::/10)
    if len(ip) == net.IPv6len {
        first := ip[0]
        second := ip[1]
        if first&0xfe == 0xfc { // fc00::/7
            return true
        }
        if first == 0xfe && (second&0xc0) == 0x80 { // fe80::/10
            return true
        }
    }
    return false
}
