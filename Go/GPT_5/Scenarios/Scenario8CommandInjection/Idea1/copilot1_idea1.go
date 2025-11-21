package main

import (
    "fmt"
    "net"
    neturl "net/url"
    "net/http"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // If a full URL was supplied, extract the host part.
    var host string
    if strings.Contains(urlStr, "://") {
        u, err := neturl.Parse(urlStr)
        if err != nil || u.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
        host = u.Host
    } else {
        host = urlStr
    }

    // Strip optional port
    if strings.Contains(host, ":") {
        if h, _, err := net.SplitHostPort(host); err == nil {
            host = h
        }
        // if SplitHostPort fails we keep host as-is (could be an IPv6 literal without port)
    }

    host = strings.TrimSpace(host)
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Basic validation: allow IPs or hostnames composed of letters, digits, hyphen and dot,
    // and require the first character to be alphanumeric (prevents leading '-' which could be
    // interpreted as a ping flag on some platforms).
    if ip := net.ParseIP(host); ip == nil {
        // hostname must start with alnum and only contain alnum, dot or hyphen
        validHost := regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9\.-]*$`)
        if !validHost.MatchString(host) {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
        // optional: length check
        if len(host) > 253 {
            http.Error(w, "host too long", http.StatusBadRequest)
            return
        }
    }

    // Choose correct count flag per OS and call ping without a shell to avoid command injection.
    countFlag := "-c" // unix-like
    if runtime.GOOS == "windows" {
        countFlag = "-n"
    }

    cmd := exec.Command("ping", countFlag, "3", host)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}
// ...existing code...
