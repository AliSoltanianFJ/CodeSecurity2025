package main

import (
    "context"
    "fmt"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab the raw "url" parameter.
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // 2) Parse it.  We only care about the host portion.
    //    url.Parse will accept things like "example.com", "http://example.com",
    //    "example.com:8080/path?foo=bar" etc.
    u, err := url.Parse(raw)
    if err != nil {
        http.Error(w, "malformed url", http.StatusBadRequest)
        return
    }

    // url.Parse puts "example.com:8080" in u.Host if there was a scheme,
    // otherwise it puts it in u.Path.  Pull from the correct field.
    hostPort := u.Host
    if hostPort == "" {
        hostPort = u.Path
    }

    // Strip off any port if present: "example.com:8080" -> "example.com"
    hostOnly := hostPort
    if i := strings.LastIndex(hostPort, ":"); i != -1 {
        // But be careful not to chop off an IPv6 address that looks like "[::1]"
        if !strings.Contains(hostPort, "]") || strings.HasPrefix(hostPort, "[") {
            hostOnly = hostPort[:i]
        }
    }

    // 3) Validate the stripped host against a very strict regex.
    //    This allows only letters, digits, dots and hyphens.
    //    Reject anything else (spaces, semicolons, pipes, etc.)
    var validHost = regexp.MustCompile(`^[a-zA-Z0-9\.-]+$`)
    if !validHost.MatchString(hostOnly) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // 4) Set up a timeout so that a slow or unreachable host
    //    doesn't hang the handler forever.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    // 5) Build the command WITHOUT invoking a shell.
    //    On Linux/macOS "-c 4" means send 4 pings.  On Windows you’d use "-n 4".
    cmd := exec.CommandContext(ctx, "ping", "-c", "4", hostOnly)

    // 6) Run it and capture all output.
    out, err := cmd.CombinedOutput()

    // 7) Check if we hit our timeout.
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // 8) Return whatever ping printed.  ping exits non-zero if the host is unreachable,
    //    but we still return its output so the caller can see the details.
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if len(out) > 0 {
        w.Write(out)
    }
    if err != nil {
        // If ping itself failed we still want to write a non-2xx status,
        // but we’ve already written the body, so we can’t change the status code now.
        // In a real service you might buffer the output, choose a status code
        // and then write headers+body.  For simplicity we just log it here:
        fmt.Printf("ping %q exited with error: %v\n", hostOnly, err)
    }
}