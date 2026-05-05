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

// hostnameRegexp matches a valid RFC1123 hostname (no protocol, no path, etc).
// We allow A–Z a–z 0–9 hyphens and dots, and require it to start/end with
// an alphanumeric.  We do NOT allow underscores, slashes, colons, spaces, etc.
var hostnameRegexp = regexp.MustCompile(`^[A-Za-z0-9](?:[A-Za-z0-9\-\.]{0,253}[A-Za-z0-9])?$`)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    fmt.Println("listening on :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        panic(err)
    }
}

// pingHandler is an HTTP handler that takes ?url=… and returns the result of
// pinging that host.  We do not shell-out to a shell, we validate the input,
// and we enforce a timeout.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "`url` query parameter is required", http.StatusBadRequest)
        return
    }

    // If the user passed something like "https://google.com/foo", pull out
    // just the hostname portion.
    host, err := extractHost(raw)
    if err != nil {
        http.Error(w, fmt.Sprintf("invalid url/host: %v", err), http.StatusBadRequest)
        return
    }

    // Do the actual ping
    out, err := ping(r.Context(), host)
    if err != nil {
        // Distinguish a timeout vs. other errors
        if err == context.DeadlineExceeded {
            http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        } else {
            http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusServiceUnavailable)
        }
        return
    }

    // Success!
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}

// extractHost takes either a raw hostname/IP or a full URL string
// and returns just the hostname portion if it’s valid.
func extractHost(raw string) (string, error) {
    // Quick sanity: if it looks like a URL (has "://") parse it
    if strings.Contains(raw, "://") {
        u, err := url.Parse(raw)
        if err != nil {
            return "", err
        }
        raw = u.Hostname()
    }

    // If it parses as an IP, accept it.
    if ip := net.ParseIP(raw); ip != nil {
        return raw, nil
    }

    // Otherwise ensure it matches our hostname regexp
    if !hostnameRegexp.MatchString(raw) {
        return "", fmt.Errorf("hostname %q did not match required pattern", raw)
    }

    return raw, nil
}

// ping runs the system "ping" command (no shell!) against the given host.
// It will send 4 echoes, and will time out after 5s (including startup).
// On success it returns the combined stdout+stderr.  On failure it returns
// whatever output we got plus a non-nil error.
func ping(parent context.Context, host string) ([]byte, error) {
    // create a context that will auto-cancel after 5 seconds
    ctx, cancel := context.WithTimeout(parent, 5*time.Second)
    defer cancel()

    // NOTE: we pass the host as a single argument to exec.Command, so there
    // is NO shell, and no chance for the user to inject extra flags or
    // commands.
    cmd := exec.CommandContext(ctx, "ping", "-c", "4", host)

    // CombinedOutput runs the command and returns stdout+stderr together.
    out, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        // If we hit our timeout, surface that specially.
        return out, context.DeadlineExceeded
    }
    return out, err
}