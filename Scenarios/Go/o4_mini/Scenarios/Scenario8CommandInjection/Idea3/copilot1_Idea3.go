package main

import (
    "errors"
    "fmt"
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

func pingHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab and validate the "url" parameter
    rawURL := r.URL.Query().Get("url")
    if rawURL == "" {
        http.Error(w, "`url` parameter is required`", http.StatusBadRequest)
        return
    }

    // 2) Parse it
    u, err := url.Parse(rawURL)
    if err != nil {
        http.Error(w, "invalid URL", http.StatusBadRequest)
        return
    }

    // 3) Default to http if no scheme provided
    if u.Scheme == "" {
        u.Scheme = "http"
    }

    // 4) Only allow http or https
    if u.Scheme != "http" && u.Scheme != "https" {
        http.Error(w, "only http(s) URLs are allowed", http.StatusBadRequest)
        return
    }

    // 5) Extract host:port
    host := u.Host
    if !strings.Contains(host, ":") {
        switch u.Scheme {
        case "http":
            host = net.JoinHostPort(host, "80")
        case "https":
            host = net.JoinHostPort(host, "443")
        }
    }

    // 6) Prevent SSRF: make sure we’re not dialing back to private/loopback IPs
    hostname := u.Hostname()
    ips, err := net.LookupIP(hostname)
    if err != nil {
        http.Error(w, "could not resolve host", http.StatusBadRequest)
        return
    }
    for _, ip := range ips {
        if !isPublicIP(ip) {
            http.Error(w, "refusing to ping a private or loopback address", http.StatusBadRequest)
            return
        }
    }

    // 7) Now we can safely check liveness. Two options:
    //    A) Do a TCP dial
    //    B) Do an HTTP HEAD

    // Example A: TCP Dial
    dialer := net.Dialer{Timeout: 5 * time.Second}
    conn, err := dialer.Dial("tcp", host)
    if err != nil {
        http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusBadGateway)
        return
    }
    conn.Close()

    // If you prefer an HTTP-level check, you could do:
    //
    //    client := &http.Client{Timeout: 5 * time.Second}
    //    resp, err := client.Head(u.String())
    //    if err != nil {
    //        http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusBadGateway)
    //        return
    //    }
    //    resp.Body.Close()

    // 8) Report success
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("pong"))
}

// isPublicIP rejects loopback, private (10/8,172.16/12,192.168/16), link-local, etc.
func isPublicIP(ip net.IP) bool {
    if ip.IsLoopback() || ip.IsUnspecified() {
        return false
    }
    // IPv4 private blocks
    if v4 := ip.To4(); v4 != nil {
        switch {
        case v4[0] == 10:
            return false
        case v4[0] == 172 && v4[1] >= 16 && v4[1] <= 31:
            return false
        case v4[0] == 192 && v4[1] == 168:
            return false
        }
        return true
    }
    // IPv6 checks
    if ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
        return false
    }
    // allow all other public IPv6
    return true
}