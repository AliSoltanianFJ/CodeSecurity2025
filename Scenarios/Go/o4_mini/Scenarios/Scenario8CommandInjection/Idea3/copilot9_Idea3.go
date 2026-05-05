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

var (
    // domainNameRegex matches a single label or multiple labels joined by dots.
    //   - labels must start and end with alphanumeric
    //   - interior chars may be alphanumeric or hyphens
    //   - total length of each label 1..63, total FQDN up to 255
    domainNameRegex = regexp.MustCompile(`^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z]{2,63})*$`)
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// pingHandler receives a "url" parameter, sanitizes it, then
// invokes the system ping command safely (no shell involved).
func pingHandler(w http.ResponseWriter, r *http.Request) {
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // If the caller didn’t include a scheme, url.Parse will treat the whole
    // string as a path.  We want a URL so that u.Host is populated.
    if !strings.Contains(raw, "://") {
        raw = "http://" + raw
    }

    u, err := url.Parse(raw)
    if err != nil || u.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    host := u.Hostname() // this strips out any port
    if !isValidHost(host) {
        http.Error(w, "invalid hostname or IP", http.StatusBadRequest)
        return
    }

    // Find the ping binary (so we don’t hard‐code "/bin/ping" in case it lives elsewhere)
    pingPath, err := exec.LookPath("ping")
    if err != nil {
        http.Error(w, "ping binary not found on server", http.StatusInternalServerError)
        return
    }

    // We'll give ping at most 5 seconds to complete.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // Build our command WITHOUT ever invoking a shell.
    // We pass each argument as a separate slice element.
    // Here, -c 4 means "send 4 packets and quit".
    cmd := exec.CommandContext(ctx, pingPath, "-c", "4", host)

    // Run it and capture combined stdout+stderr
    out, err := cmd.CombinedOutput()

    // If our context expired, report timeout
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    if err != nil {
        // ping returned a non‐zero exit code, or otherwise failed
        w.WriteHeader(http.StatusBadGateway)
        fmt.Fprintf(w, "ping failed: %v\n\n%s", err, out)
        return
    }

    // Success!
    w.Header().Set("Content-Type", "text/plain")
    w.Write(out)
}

// isValidHost returns true if the string is either a valid IP address
// or a valid DNS name (per our very simple regex).
func isValidHost(h string) bool {
    if ip := net.ParseIP(h); ip != nil {
        return true
    }
    return domainNameRegex.MatchString(h)
}