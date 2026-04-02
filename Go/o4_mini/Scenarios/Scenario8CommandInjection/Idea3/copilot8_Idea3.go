package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "net/url"
    "os"
    "os/signal"
    "regexp"
    "runtime"
    "strings"
    "syscall"
    "time"
    "os/exec"
)

var (
    // very conservative domain‐name regexp from the RFCs:
    // labels joined by dots, each label 1–63 chars, overall <= 253 chars
    domainRegexp = regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
)

func main() {
    // graceful shutdown support (optional)
    stop := make(chan os.Signal, 1)
    signal.Notify(stop, os.Interrupt, syscall.SIGTERM)

    http.HandleFunc("/url/ping", pingHandler)

    srv := &http.Server{Addr: ":8080"}

    go func() {
        if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
            fmt.Fprintf(os.Stderr, "server error: %v\n", err)
            os.Exit(1)
        }
    }()

    <-stop
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    srv.Shutdown(ctx)
}

// pingHandler pings the user‐supplied “url” (really just host or IP) using the system ping.
// It escapes all user data by passing a []string of args to exec.CommandContext,
// and never invokes a shell, so there is no way to inject extra flags or commands.
func pingHandler(w http.ResponseWriter, r *http.Request) {
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // 1) Parse as URL if it looks like one; else treat as bare host/IP
    var hostport string
    if strings.Contains(raw, "://") {
        u, err := url.Parse(raw)
        if err != nil || u.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
        hostport = u.Host
    } else {
        hostport = raw
    }

    // 2) strip any port (ping doesn't use it anyway)
    host, _, err := net.SplitHostPort(hostport)
    if err != nil {
        // no port present, ok
        host = hostport
    }

    // 3) Validate that host is either a valid IP or a valid domain name
    if ip := net.ParseIP(host); ip == nil {
        // not an IP, so must be a domain
        if len(host) > 253 || !domainRegexp.MatchString(host) {
            http.Error(w, "invalid hostname", http.StatusBadRequest)
            return
        }
    }

    // 4) Build ping command safely (no shell involved)
    var args []string
    // ping -c 4 on unix, ping -n 4 on windows
    if runtime.GOOS == "windows" {
        args = []string{"-n", "4", host}
    } else {
        args = []string{"-c", "4", host}
    }

    // 5) Enforce a hard timeout via Context
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", args...)
    output, err := cmd.CombinedOutput()

    // 6) Check for timeout
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // 7) Other execution errors
    if err != nil {
        // we return a 502 Bad Gateway to indicate our upstream ping failed
        w.WriteHeader(http.StatusBadGateway)
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.Write(output)
        return
    }

    // 8) Success – return the raw ping output
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write(output)
}