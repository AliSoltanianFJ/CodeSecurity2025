package main

import (
    "context"
    "fmt"
    "net/http"
    "net/url"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using an HTTP request (safe alternative to running system "ping") */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse and normalize the URL. If no scheme provided, assume http.
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        parsed, err = url.Parse("http://" + urlParam)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Only allow http(s) schemes to avoid handing off to other protocols.
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported url scheme", http.StatusBadRequest)
        return
    }

    // Use a context with timeout to avoid hanging requests.
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, "HEAD", parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    client := &http.Client{}
    resp, err := client.Do(req)
    if err != nil {
        http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusServiceUnavailable)
        return
    }
    defer resp.Body.Close()

    fmt.Fprintf(w, "reachable (status %s)\n", resp.Status)
}
// ...existing code...
