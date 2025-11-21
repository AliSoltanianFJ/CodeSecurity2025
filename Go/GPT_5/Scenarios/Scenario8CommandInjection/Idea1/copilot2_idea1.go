package main

import (
    "fmt"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Try to extract host if a full URL was provided
    host := urlParam
    if u, err := url.Parse(urlParam); err == nil && u.Host != "" {
        host = u.Host
    }

    // Strip any port if present
    if idx := strings.LastIndex(host, ":"); idx != -1 {
        // only remove port if it looks like host:port (not an IPv6 without brackets)
        // handle [ipv6]:port and plain host:port
        if strings.HasPrefix(host, "[") && strings.Contains(host, "]") {
            // leave as-is for now; url.Parse normally returns host including brackets for IPv6
            // we handle by trimming optional port after closing bracket
            if closing := strings.Index(host, "]"); closing != -1 && len(host) > closing+1 && host[closing+1] == ':' {
                host = host[:closing]
                host = strings.TrimPrefix(host, "[")
                host = strings.TrimSuffix(host, "]")
            }
        } else if !strings.Contains(host, "]") { // simple hostname:port or ipv4:port
            host = strings.Split(host, ":")[0]
        }
    }

    // Basic validation: allow IPs or DNS names made of letters, digits, hyphen and dots
    if ip := net.ParseIP(host); ip == nil {
        var validHost = regexp.MustCompile(`^[a-zA-Z0-9-\.]+$`)
        if !validHost.MatchString(host) || len(host) > 253 {
            http.Error(w, "invalid hostname", http.StatusBadRequest)
            return
        }
    }

    // Choose the ping count flag depending on OS
    countFlag := "-c"
    if runtime.GOOS == "windows" {
        countFlag = "-n"
    }

    // Run ping without a shell to avoid command injection
    cmd := exec.Command("ping", countFlag, "3", host)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write(out)
}
```//
