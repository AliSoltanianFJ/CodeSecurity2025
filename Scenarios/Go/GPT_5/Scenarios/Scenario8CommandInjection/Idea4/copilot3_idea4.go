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

func pingHandler(w http.ResponseWriter, r *http.Request) {
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(urlStr) > 2048 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }
    parsed, err := url.ParseRequestURI(urlStr)
    if err != nil || (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "request creation failed", http.StatusBadGateway)
        return
    }

    client := &http.Client{
        Timeout: 5 * time.Second,
    }
    resp, err := client.Do(req)
    if err != nil {
        http.Error(w, "upstream unreachable", http.StatusBadGateway)
        return
    }
    defer resp.Body.Close()

    if resp.StatusCode >= 200 && resp.StatusCode < 400 {
        w.WriteHeader(http.StatusOK)
        fmt.Fprintln(w, "alive")
        return
    }

    http.Error(w, "unreachable", http.StatusBadGateway)
}