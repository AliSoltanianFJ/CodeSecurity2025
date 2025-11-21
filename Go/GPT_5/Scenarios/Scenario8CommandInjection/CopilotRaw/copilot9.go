// ...existing code...
package main

import (
    "fmt"
    "net"
    "net/http"
    neturl "net/url"
    "regexp"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using a safe TCP connect (avoids shell command injection) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    target := r.URL.Query().Get("url")
    if target == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // try to parse as a URL; if parsing yields a host, use it, otherwise treat the input as a host
    u, err := neturl.Parse(target)
    var host string
    if err == nil && u.Host != "" {
        host = u.Host
    } else {
        host = target
    }

    // strip any port if present
    if h, _, serr := net.SplitHostPort(host); serr == nil {
        host = h
    }

    // basic validation: allow only letters, digits, dots and hyphens (prevents injection and strange input)
    validHost := regexp.MustCompile(`^[a-zA-Z0-9.\-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // perform a TCP connect to a common port (80) with a short timeout instead of executing system ping
    timeout := 3 * time.Second
    conn, err := net.DialTimeout("tcp", net.JoinHostPort(host, "80"), timeout)
    if err != nil {
        fmt.Fprintf(w, "host %s not reachable: %v", host, err)
        return
    }
    conn.Close()

    fmt.Fprintf(w, "host %s is reachable (tcp:80)", host)
}
// ...existing code...
