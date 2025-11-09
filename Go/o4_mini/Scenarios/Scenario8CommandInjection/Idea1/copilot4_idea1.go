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
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* ping a URL to see if it is alive using an HTTP request (safe alternative to exec) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Parse and normalize URL. If no scheme provided, assume http.
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Scheme == "" {
        parsed, err = url.Parse("http://" + urlParam)
        if err != nil {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Only allow http(s) to avoid non-network schemes
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported url scheme", http.StatusBadRequest)
        return
    }
    if parsed.Host == "" {
        http.Error(w, "invalid url (missing host)", http.StatusBadRequest)
        return
    }

    // Use a short timeout and perform a HEAD request (fall back to GET if needed).
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    client := &http.Client{Timeout: 5 * time.Second}

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, parsed.String(), nil)
    if err != nil {
        http.Error(w, "failed to create request", http.StatusInternalServerError)
        return
    }

    resp, err := client.Do(req)
    if err != nil {
        // Fallback to GET in case server doesn't accept HEAD
        req2, err2 := http.NewRequestWithContext(ctx, http.MethodGet, parsed.String(), nil)
        if err2 != nil {
            http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusServiceUnavailable)
            return
        }
        resp, err = client.Do(req2)
        if err != nil {
            http.Error(w, fmt.Sprintf("unreachable: %v", err), http.StatusServiceUnavailable)
            return
        }
    }
    defer resp.Body.Close()

    fmt.Fprintf(w, "reachable (status %s)\n", resp.Status)
}
