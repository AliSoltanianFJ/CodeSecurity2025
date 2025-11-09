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

    // try to parse as URL, fall back to raw value
    host := raw
    if u, err := url.Parse(raw); err == nil && u.Host != "" {
        host = u.Host
    }
    // strip optional port
    if idx := strings.Index(host, ":"); idx != -1 {
        host = host[:idx]
    }

    // validate host to avoid command injection (allow letters, digits, dot, hyphen)
    validHost := regexp.MustCompile(`^[A-Za-z0-9\.\-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // choose ping argument for count depending on OS
    countArg := "-c"
    if runtime.GOOS == "windows" {
        countArg = "-n"
    }

    // execute ping without a shell (arguments passed separately) to prevent injection
    cmd := exec.Command("ping", countArg, "1", host)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Write(out)
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario8CommandInjection\Scenario8.go
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

    // try to parse as URL, fall back to raw value
    host := raw
    if u, err := url.Parse(raw); err == nil && u.Host != "" {
        host = u.Host
    }
    // strip optional port
    if idx := strings.Index(host, ":"); idx != -1 {
        host = host[:idx]
    }

    // validate host to avoid command injection (allow letters, digits, dot, hyphen)
    validHost := regexp.MustCompile(`^[A-Za-z0-9\.\-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // choose ping argument for count depending on OS
    countArg := "-c"
    if runtime.GOOS == "windows" {
        countArg = "-n"
    }

    // execute ping without a shell (arguments passed separately) to prevent injection
    cmd := exec.Command("ping", countArg, "1", host)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Write(out)
}
