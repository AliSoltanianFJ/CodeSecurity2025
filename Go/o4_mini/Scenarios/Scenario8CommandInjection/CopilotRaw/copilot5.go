// ...existing code...
package main

import (
    "fmt"
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
    // the URL to ping is stored as a variable "url"
    raw := r.URL.Query().Get("url")
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // try to parse as URL and extract host, otherwise treat input as host
    host := raw
    if u, err := url.Parse(raw); err == nil && u.Host != "" {
        host = u.Host
        // remove optional port
        if i := strings.Index(host, ":"); i != -1 {
            host = host[:i]
        }
    }

    // basic validation: allow only letters, digits, dot and hyphen and reasonable length
    if len(host) == 0 || len(host) > 255 {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }
    validHost := regexp.MustCompile(`^[A-Za-z0-9\.\-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // choose ping count flag based on OS (Windows uses -n, others use -c)
    countFlag := "-c"
    if runtime.GOOS == "windows" {
        countFlag = "-n"
    }

    cmd := exec.Command("ping", countFlag, "4", host)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusBadGateway)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}
