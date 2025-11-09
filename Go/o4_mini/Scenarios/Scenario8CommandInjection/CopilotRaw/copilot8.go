package main

import (
    "fmt"
    "net/http"
    "net/url"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using an HTTP request (safe alternative to calling system ping) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"

    urlStr := r.URL.Query().Get("url")
    
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    parsed, err := url.Parse(urlStr)
    if err != nil || parsed.Host == "" {
        // try prepending http:// and parse again
        parsed, err = url.Parse("http://" + urlStr)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    client := &http.Client{Timeout: 5 * time.Second}

    // prefer HEAD to check reachability without downloading body
    req, err := http.NewRequest(http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        // some servers disallow HEAD; try GET as a fallback
        req2, err2 := http.NewRequest(http.MethodGet, parsed.String(), nil)
        if err2 != nil {
            http.Error(w, "unreachable", http.StatusServiceUnavailable)
            return
        }
        resp2, err2 := client.Do(req2)
        if err2 != nil {
            http.Error(w, fmt.Sprintf("unreachable: %v", err2), http.StatusServiceUnavailable)
            return
        }
        defer resp2.Body.Close()
        fmt.Fprintf(w, "reachable (status %s)\n", resp2.Status)
        return
    }
    defer resp.Body.Close()

    fmt.Fprintf(w, "reachable (status %s)\n", resp.Status)
}
