// ...existing code...
package main

import (
    "net"
    neturl "net/url"
    "net/http"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
    "log"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url"
    target := r.URL.Query().Get("url")
    if strings.TrimSpace(target) == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // If the user passed a full URL, extract host; otherwise treat as host
    u, err := neturl.Parse(target)
    var host string
    if err == nil && u.Host != "" {
        host = u.Host
    } else {
        host = target
    }
    host = strings.TrimSpace(host)

    // Remove optional port (host:port) and IPv6 brackets
    if h, _, err := net.SplitHostPort(host); err == nil {
        host = h
    }
    host = strings.Trim(host, "[]")

    // Validate host: allow IP addresses or simple domain names (letters, digits, hyphen, dot)
    if net.ParseIP(host) == nil {
        re := regexp.MustCompile(`^[A-Za-z0-9.-]+$`)
        if !re.MatchString(host) || strings.HasPrefix(host, "-") || strings.HasSuffix(host, "-") {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
    }

    // Choose ping count flag depending on OS (Windows uses -n, Unix uses -c)
    countFlag := "-c"
    if runtime.GOOS == "windows" {
        countFlag = "-n"
    }

    // Execute ping without a shell (avoids shell injection)
    cmd := exec.Command("ping", countFlag, "4", host)
    output, err := cmd.CombinedOutput()
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if err != nil {
        http.Error(w, "ping failed: "+err.Error()+"\n\n"+string(output), http.StatusInternalServerError)
        return
    }

    w.Write(output)
}
// ...existing code...
