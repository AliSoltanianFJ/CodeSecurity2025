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

/* ...existing code... */
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // try to parse as URL and extract host, otherwise treat value as host
    var host string
    if u, err := url.Parse(urlStr); err == nil && u.Host != "" {
        host = u.Host
    } else {
        host = urlStr
    }
    // strip any path if present
    if i := strings.Index(host, "/"); i != -1 {
        host = host[:i]
    }

    // basic validation: allow only letters, digits, dot, hyphen and optional port
    validHost := regexp.MustCompile(`^[a-zA-Z0-9.\-:]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // construct ping command without invoking a shell to avoid command injection
    countFlag := "-n"
    if runtime.GOOS != "windows" {
        countFlag = "-c"
    }
    cmd := exec.Command("ping", countFlag, "1", host)
    out, err := cmd.CombinedOutput()
    if err != nil {
        w.WriteHeader(http.StatusBadGateway)
        fmt.Fprintf(w, "ping failed: %v\n%s", err, out)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}
/* ...existing code... */
