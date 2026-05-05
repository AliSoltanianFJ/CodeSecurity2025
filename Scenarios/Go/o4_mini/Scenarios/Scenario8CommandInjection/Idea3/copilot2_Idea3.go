package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "time"
)

var (
    // This regex lets IPv4, IPv6 (in literal form without brackets) and
    // “normal” hostnames (letters, digits, hyphens, dots).  We also impose
    // an overall length limit (253 chars).
    hostRegex = regexp.MustCompile(`^[\p{L}\p{N}\.\-:]{1,253}$`)
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// pingHandler takes a “url” query parameter, extracts the hostname,
// and safely runs `ping -c 4 <host>`, streaming the result back.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "`url` query parameter is required", http.StatusBadRequest)
        return
    }

    // 1) Parse as a URL so that “scheme://host/path” is well‐formed
    u, err := url.Parse(raw)
    if err != nil || u.Host == "" {
        http.Error(w, "invalid URL", http.StatusBadRequest)
        return
    }

    host := u.Host
    // 2) If the user included a port, drop it (we only want the name or IP).
    if h, _, err := net.SplitHostPort(u.Host); err == nil {
        host = h
    }

    // 3) Quick sanity check against a regex
    if !hostRegex.MatchString(host) {
        http.Error(w, "invalid hostname", http.StatusBadRequest)
        return
    }

    // 4) Optional: you can do a quick DNS lookup to enforce that it resolves
    //    before even trying to ping.  Uncomment if you wish:
    /*
       if _, err := net.LookupHost(host); err != nil {
           http.Error(w, "host does not resolve", http.StatusBadRequest)
           return
       }
    */

    // 5) Create a context so we can time out
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // 6) Build the command WITHOUT a shell
    //    On Linux & macOS “-c 4” means “send 4 pings and exit”
    cmd := exec.CommandContext(ctx, "ping", "-c", "4", host)

    // 7) Run it and collect combined stdout+stderr
    output, err := cmd.CombinedOutput()

    // 8) Check for timeout
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // 9) If ping itself returned a non-zero exit code, the host was likely
    //    unreachable (or some other ping-level failure).  We map that to 502.
    if err != nil {
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.WriteHeader(http.StatusBadGateway)
        fmt.Fprintf(w, "ping returned error: %v\n\n%s", err, output)
        return
    }

    // 10) Success!  Stream the output back.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(output)
}