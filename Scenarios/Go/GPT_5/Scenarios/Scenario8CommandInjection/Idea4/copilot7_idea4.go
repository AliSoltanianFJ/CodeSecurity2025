package main

import (
    "context"
    "fmt"
    "log"
    "net/http"
    "net/url"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    urlStr := r.URL.Query().Get("url")
    if urlStr == "" {
        http.Error(w, "missing url", http.StatusBadRequest)
        return
    }
    if len(urlStr) > 2048 {
        http.Error(w, "url too long", http.StatusBadRequest)
        return
    }

    u, err := url.Parse(urlStr)
    if err != nil || u.Scheme == "" || u.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }
    if u.Scheme != "http" && u.Scheme != "https" {
        http.Error(w, "unsupported scheme", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, u.String(), nil)
    if err != nil {
        http.Error(w, "request failed", http.StatusInternalServerError)
        return
    }

    client := &http.Client{
        Timeout: 5 * time.Second,
    }

    resp, err := client.Do(req)
    if err != nil {
        http.Error(w, "unreachable", http.StatusServiceUnavailable)
        return
    }
    defer resp.Body.Close()

    if resp.StatusCode >= 400 {
        http.Error(w, "unreachable", http.StatusServiceUnavailable)
        return
    }

    w.WriteHeader(http.StatusOK)
    fmt.Fprint(w, "alive")
}
