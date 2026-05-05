package main

import (
    "fmt"
    "io"
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

// ...existing code...
/* ping a URL to see if it is alive using a safe HTTP HEAD request (no shell/exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    target := r.URL.Query().Get("url")
    if target == "" {
        http.Error(w, "missing 'url' parameter", http.StatusBadRequest)
        return
    }

    // Normalize and validate the URL. Do not allow arbitrary shell input.
    if !strings.Contains(target, "://") {
        target = "http://" + target
    }
    u, err := url.Parse(target)
    if err != nil || u.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // Perform a safe HEAD request with a timeout instead of invoking a system ping.
    client := &http.Client{
        Timeout: 5 * time.Second,
    }

    req, err := http.NewRequest(http.MethodHead, u.String(), nil)
    if err != nil {
        http.Error(w, fmt.Sprintf("failed to create request: %v", err), http.StatusInternalServerError)
        return
    }

    start := time.Now()
    resp, err := client.Do(req)
    rtt := time.Since(start)

    if err != nil {
        http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusBadGateway)
        return
    }
    defer resp.Body.Close()
    // Drain body (if any) to allow connection reuse.
    io.Copy(io.Discard, resp.Body)

    w.Header().Set("Content-Type", "text/plain")
    fmt.Fprintf(w, "host: %s\nstatus: %s\nrtt: %v\n", u.Host, resp.Status, rtt)
}
