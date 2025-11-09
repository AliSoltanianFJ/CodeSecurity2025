// ...existing code...
package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "net/url"
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
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // try to extract host from a full URL, otherwise treat the param as host
    host := urlParam
    if u, err := url.Parse(urlParam); err == nil {
        if u.Host != "" {
            host = u.Host
        } else if u.Path != "" && (strings.Contains(u.Path, ".") || strings.Contains(u.Path, ":")) {
            // sometimes a bare host is placed in Path
            host = u.Path
        }
    }

    // strip optional port
    if strings.Contains(host, ":") {
        host = strings.Split(host, ":")[0]
    }
    host = strings.TrimSpace(host)
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // validate host: allow letters, digits, hyphen and dot only
    validHost := regexp.MustCompile(`^[a-zA-Z0-9\.-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host format", http.StatusBadRequest)
        return
    }

    // optional: ensure the host resolves to mitigate some injection / wrong inputs
    if _, err := net.LookupIP(host); err != nil {
        http.Error(w, fmt.Sprintf("host lookup failed: %v", err), http.StatusBadRequest)
        return
    }

    // pick ping args depending on OS (Windows uses -n, Unix uses -c)
    var args []string
    if runtime.GOOS == "windows" {
        args = []string{"-n", "4", host}
    } else {
        args = []string{"-c", "4", host}
    }

    // run ping without a shell (prevents command injection) and with a timeout
    ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
    defer cancel()
    cmd := exec.CommandContext(ctx, "ping", args...)
    out, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}
...existing code...
