// ...existing code...
package main

import (
    "context"
    "fmt"
    "net/http"
    "net/url"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using safe HTTP request */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url"
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" || len(urlStr) > 2048 {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    parsed, err := url.ParseRequestURI(urlStr)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "service unavailable", http.StatusBadGateway)
        return
    }

    client := &http.Client{
        Timeout: 5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            return http.ErrUseLastResponse
        },
    }

    resp, err := client.Do(req)
    if err != nil {
        http.Error(w, "target unreachable", http.StatusBadGateway)
        return
    }
    defer resp.Body.Close()

    if resp.StatusCode >= 200 && resp.StatusCode < 400 {
        w.WriteHeader(http.StatusOK)
        fmt.Fprintln(w, "target reachable")
        return
    }

    http.Error(w, "target returned error", http.StatusBadGateway)
}
// ...existing code...
