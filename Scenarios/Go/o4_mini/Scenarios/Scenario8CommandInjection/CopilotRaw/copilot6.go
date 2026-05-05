// ...existing code...
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

/* ping a URL to see if it is alive using a safe HTTP HEAD request (avoids shelling out) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url"
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // If the user didn't provide a scheme, assume http
    parsed, err := url.Parse(urlParam)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        // try prepending http:// and parse again
        parsed, err = url.Parse("http://" + urlParam)
        if err != nil || parsed.Host == "" {
            http.Error(w, "invalid url", http.StatusBadRequest)
            return
        }
    }

    // Use a short timeout and perform an HTTP HEAD request instead of calling system ping.
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
