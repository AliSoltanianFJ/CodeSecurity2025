package main

import (
    "fmt"
    "net/http"
    "os/exec"
    "net/url"
    "runtime"
    "regexp"
    "strings"
    "net"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    parsed, err := url.Parse(urlStr)
    var host string
    if err == nil && parsed.Host != "" {
        host = parsed.Host
    } else {
        // allow users to pass bare hostnames/IPs as well
        host = urlStr
    }

    // strip optional port if present
    if strings.Contains(host, ":") {
        if h, _, err := net.SplitHostPort(host); err == nil {
            host = h
        }
    }

    host = strings.TrimSpace(host)

    // validate host: allow only letters, digits, dot and hyphen
    validHost := regexp.MustCompile(`^[a-zA-Z0-9\.\-]+$`)
    if !validHost.MatchString(host) || len(host) == 0 || len(host) > 253 {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // build command safely by passing host as a single argument (prevents shell interpretation)
    var cmd *exec.Cmd
    if runtime.GOOS == "windows" {
        cmd = exec.Command("ping", "-n", "1", host)
    } else {
        cmd = exec.Command("ping", "-c", "1", host)
    }

    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}
// ...existing code...
