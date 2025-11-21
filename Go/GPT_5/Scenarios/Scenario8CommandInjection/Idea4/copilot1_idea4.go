package main

import (
    "log"
    "net/http"
    "net/url"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...
/* ping a URL to see if it is alive using safe HTTP requests */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" || len(urlParam) > 2048 {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    u, err := url.Parse(urlParam)
    if err != nil || (u.Scheme != "http" && u.Scheme != "https") || u.Host == "" || u.User != nil {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    client := &http.Client{
        Timeout: 5 * time.Second,
        CheckRedirect: func(req *http.Request, via []*http.Request) error {
            return http.ErrUseLastResponse
        },
    }

    req, err := http.NewRequest(http.MethodHead, u.String(), nil)
    if err != nil {
        http.Error(w, "unreachable", http.StatusBadGateway)
        return
    }
    req.Header.Set("User-Agent", "ping-check/1.0")

    resp, err := client.Do(req)
    if resp != nil && resp.Body != nil {
        resp.Body.Close()
    }
    if err != nil || resp.StatusCode >= 400 {
        // fallback to GET for servers that don't allow HEAD
        req2, err2 := http.NewRequest(http.MethodGet, u.String(), nil)
        if err2 == nil {
            req2.Header.Set("User-Agent", "ping-check/1.0")
            resp2, err2 := client.Do(req2)
            if resp2 != nil && resp2.Body != nil {
                resp2.Body.Close()
            }
            if err2 == nil && resp2.StatusCode < 400 {
                w.WriteHeader(http.StatusOK)
                w.Write([]byte("alive"))
                return
            }
        }
        http.Error(w, "unreachable", http.StatusBadGateway)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("alive"))
}
// ...existing code...
