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
    // ...existing code...
    // the URL to ping is stored as a variable "url"
    url := strings.TrimSpace(r.URL.Query().Get("url"))

    if url == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Validate input to avoid command injection:
    // allow either a valid IP or a hostname consisting of letters, digits, hyphens and dots.
    if net.ParseIP(url) == nil {
        validHost := regexp.MustCompile(`^[a-zA-Z0-9.-]{1,255}$`)
        if !validHost.MatchString(url) || strings.HasPrefix(url, ".") || strings.HasSuffix(url, ".") {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
    }

    // Build command without invoking a shell (avoid "sh -c" / "cmd /C")
    var args []string
    if runtime.GOOS == "windows" {
        args = []string{"-n", "3", url}
    } else {
        args = []string{"-c", "3", url}
    }

    // Use a context with timeout to prevent long-running commands
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", args...)
    out, err := cmd.CombinedOutput()

    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
    // ...existing code...
}
