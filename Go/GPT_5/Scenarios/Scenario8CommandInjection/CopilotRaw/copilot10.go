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
    //the URL to ping is stored as a variable "url" 
    urlParam := r.URL.Query().Get("url")
    // ...existing code...

    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // If a full URL was provided, extract the host.
    host := urlParam
    if strings.Contains(host, "://") {
        parsed, err := url.Parse(host)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
        host = parsed.Host
    }

    // Strip optional port
    if idx := strings.LastIndex(host, ":"); idx != -1 {
        // if there's a ']' it's an IPv6 literal with port, keep IPv6 content
        if !strings.HasPrefix(host, "[") {
            host = host[:idx]
        }
    }

    // Basic whitelist validation: allow letters, digits, dot and hyphen
    validHost := regexp.MustCompile(`^[a-zA-Z0-9\.\-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Choose the ping count flag depending on OS (Windows uses -n, others use -c)
    countFlag := "-c"
    if runtime.GOOS == "windows" {
        countFlag = "-n"
    }

    // Use exec.Command with separate args (avoids shell interpretation)
    cmd := exec.Command("ping", countFlag, "4", host)
    output, err := cmd.CombinedOutput()
    if err != nil {
        // Return both the command error and any produced output for debugging
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "ping error: %v\n\n%s", err, output)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write(output)
}
```// filepath:
