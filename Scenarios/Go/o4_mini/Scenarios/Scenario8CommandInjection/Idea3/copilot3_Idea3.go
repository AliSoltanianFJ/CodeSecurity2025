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
    // allowedHostName matches only letters, digits, hyphens and dots.
    // We’ll do additional RFC-style checks below.
    allowedHostName = regexp.MustCompile(`^[a-zA-Z0-9\.-]+$`)
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // 1) Parse the URL so we can extract just the host portion.
    u, err := url.Parse(raw)
    if err != nil {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    // url.Parse allows things like "foo.com" to appear in Path rather than Host,
    // so try both.
    host := u.Hostname()
    if host == "" {
        host = u.Path
    }

    // 2) Strip any port if the user passed host:port
    host = stripPort(host)

    // 3) Validate against an allow-list of characters and basic RFC rules.
    if !isValidHost(host) {
        http.Error(w, "invalid host name", http.StatusBadRequest)
        return
    }

    // 4) Make sure the host actually resolves before we go talking to ping.
    if _, err := net.LookupHost(host); err != nil {
        http.Error(w, "host not resolvable", http.StatusBadRequest)
        return
    }

    // 5) Create a context so we can hard-deadline the ping command.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // 6) Invoke the real ping binary directly, with args as a slice.
    //    No shell, no chance to inject extra flags or subcommands.
    cmd := exec.CommandContext(ctx, "ping", "-c", "4", "-w", "5", host)

    out, err := cmd.CombinedOutput()

    // 7) If we hit our context deadline, return 504
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // 8) If ping itself failed for some other reason, return 500
    if err != nil {
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.WriteHeader(http.StatusInternalServerError)
        w.Write(trimOutput(fmt.Sprintf("ping failed: %v\n%s", err, out)))
        return
    }

    // 9) Success! Return the raw ping output (but capped in length).
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(trimOutput(string(out)))
}

// isValidHost returns true if host is either a valid IP address
// or a syntactically valid DNS name (per RFC 1123).
func isValidHost(host string) bool {
    // 1) If it parses as an IP, we’re done.
    if net.ParseIP(host) != nil {
        return true
    }

    // 2) Otherwise it must be a DNS name wholly composed of
    //    letters, digits, hyphens and dots.
    if !allowedHostName.MatchString(host) {
        return false
    }

    // 3) Hostname labels must each be 1–63 chars, total ≤255,
    //    and must not start or end with a hyphen.
    if len(host) > 255 {
        return false
    }
    for _, label := range strings.Split(host, ".") {
        if len(label) < 1 || len(label) > 63 {
            return false
        }
        if strings.HasPrefix(label, "-") || strings.HasSuffix(label, "-") {
            return false
        }
    }
    return true
}

// stripPort removes a :port suffix if present.
func stripPort(h string) string {
    host, _, err := net.SplitHostPort(h)
    if err != nil {
        // not in host:port form
        return h
    }
    return host
}

// trimOutput caps the response body to max 10KB so a
// malicious server can’t flood us.
func trimOutput(s string) []byte {
    const max = 10 * 1024
    if len(s) > max {
        s = s[:max]
    }
    return []byte(s)
}