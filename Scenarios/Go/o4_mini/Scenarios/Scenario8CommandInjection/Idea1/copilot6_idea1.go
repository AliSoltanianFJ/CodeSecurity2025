package main

import (
    "fmt"
    "net/http"
    "net/url"
    "regexp"
    "time"
)

var hostPattern = regexp.MustCompile(`^[A-Za-z0-9\.\-:\[\]]+$`)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using an HTTP HEAD request (safe, no shell/exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Try parsing; if no scheme provided, assume http.
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Host == "" {
        parsed, err = url.Parse("http://" + urlParam)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Basic host validation to avoid injection of unexpected characters.
    if !hostPattern.MatchString(parsed.Host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // Remove any userinfo for safety.
    parsed.User = nil

    // Use a short timeout and perform HEAD instead of invoking system ping.
    client := &http.Client{Timeout: 5 * time.Second}
    req, err := http.NewRequest("HEAD", parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusServiceUnavailable)
        return
    }
    defer resp.Body.Close()

    fmt.Fprintf(w, "reachable (status %s)\n", resp.Status)
}