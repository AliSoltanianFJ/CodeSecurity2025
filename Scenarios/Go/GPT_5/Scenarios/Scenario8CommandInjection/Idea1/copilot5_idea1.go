package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    host := r.URL.Query().Get("url")
    if host == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Basic validation: only allow letters, digits, dot and hyphen
    // Reject anything that could be used to inject shell metacharacters
    if len(host) > 255 {
        http.Error(w, "host too long", http.StatusBadRequest)
        return
    }
    if strings.HasPrefix(host, ".") || strings.HasSuffix(host, ".") || strings.Contains(host, "..") {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }
    validHost := regexp.MustCompile(`^[A-Za-z0-9.-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Ensure the host resolves (prevents obvious misuse)
    if net.ParseIP(host) == nil {
        if _, err := net.LookupHost(host); err != nil {
            http.Error(w, "host lookup failed", http.StatusBadRequest)
            return
        }
    }

    // Use a context with timeout and exec.Command (no shell) to avoid injection
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    var cmd *exec.Cmd
    if runtime.GOOS == "windows" {
        cmd = exec.CommandContext(ctx, "ping", "-n", "3", host)
    } else {
        cmd = exec.CommandContext(ctx, "ping", "-c", "3", host)
    }

    out, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write(out)
}