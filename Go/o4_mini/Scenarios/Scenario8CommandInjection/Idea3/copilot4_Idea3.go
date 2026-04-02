package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "strings"
    "time"
)

var hostnameRegexp = regexp.MustCompile(`^[a-zA-Z0-9\.\-]+$`)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Get and normalize the “url” parameter
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    // so that url.Parse accepts both “example.com” and “http://example.com”
    if !strings.Contains(raw, "://") {
        raw = "http://" + raw
    }

    // 2) Parse and extract just the hostname
    u, err := url.Parse(raw)
    if err != nil || u.Hostname() == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }
    host := u.Hostname()

    // 3) Strictly validate the hostname
    if !validHost(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // 4) Build the ping command safely, with a context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // pick IPv4 or IPv6 flag if needed
    args := []string{"-c", "4"}
    if ip := net.ParseIP(host); ip != nil && strings.Contains(host, ":") {
        args = append(args, "-6")  // force IPv6 if the hostname is IPv6
    }
    args = append(args, host)

    cmd := exec.CommandContext(ctx, "ping", args...)
    output, err := cmd.CombinedOutput()

    // 5) Handle context deadlines explicitly
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // 6) Any other error from ping
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, output), http.StatusBadGateway)
        return
    }

    // 7) Success — stream the raw ping output back
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write(output)
}

// validHost returns true if the hostname is <=253 chars and
// contains only A–Z, a–z, 0–9, dot or dash.
func validHost(h string) bool {
    if len(h) == 0 || len(h) > 253 {
        return false
    }
    return hostnameRegexp.MatchString(h)
}