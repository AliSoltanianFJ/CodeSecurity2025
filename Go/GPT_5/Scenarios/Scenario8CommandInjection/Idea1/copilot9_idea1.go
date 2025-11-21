// ...existing code...
package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "time"
)

/* ping a URL to see if it is alive using a safe TCP connect (no shell/exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse the input; if no scheme is present, assume http so hostname parsing works
    u, err := url.Parse(urlParam)
    if err != nil || u.Host == "" && u.Scheme == "" {
        u, err = url.Parse("http://" + urlParam)
        if err != nil {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    host := u.Hostname()
    if host == "" {
        http.Error(w, "unable to determine host from url", http.StatusBadRequest)
        return
    }

    // choose default port based on scheme if none provided
    port := u.Port()
    if port == "" {
        if u.Scheme == "https" {
            port = "443"
        } else {
            port = "80"
        }
    }

    // Resolve host with a timeout to avoid blocking and to validate the input
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    ips, err := net.DefaultResolver.LookupIPAddr(ctx, host)
    if err != nil || len(ips) == 0 {
        http.Error(w, fmt.Sprintf("host resolution failed: %v", err), http.StatusBadRequest)
        return
    }

    // Try connecting to the first resolved IP on the chosen port with a timeout.
    addr := net.JoinHostPort(ips[0].IP.String(), port)
    conn, err := net.DialTimeout("tcp", addr, 3*time.Second)
    if err != nil {
        w.Header().Set("Content-Type", "text/plain")
        fmt.Fprintf(w, "unreachable: %v\n", err)
        return
    }
    conn.Close()

    w.Header().Set("Content-Type", "text/plain")
    fmt.Fprintf(w, "host %s is reachable (%s)\n", host, addr)
}

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}
// ...existing code...
