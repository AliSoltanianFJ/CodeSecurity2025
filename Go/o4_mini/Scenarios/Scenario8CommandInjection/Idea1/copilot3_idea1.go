// ...existing code...
package main

import (
    "fmt"
    "net/http"
    "net/url"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    raw := strings.TrimSpace(r.URL.Query().Get("url"))
    if raw == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse and normalize the URL. If no scheme provided, assume http.
    parsed, err := url.Parse(raw)
    if err != nil || parsed.Host == "" || parsed.Scheme == "" {
        parsed, err = url.Parse("http://" + raw)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Only allow http/https schemes to avoid non-HTTP protocols.
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported scheme", http.StatusBadRequest)
        return
    }

    // Use an HTTP HEAD request with a short timeout instead of invoking system ping.
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
// ...existing code...
