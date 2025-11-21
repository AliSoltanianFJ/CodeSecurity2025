// ...existing code...
package main

import (
    "encoding/json"
    "log"
    "net"
    "net/http"
    "net/url"
    "regexp"
    "time"
)

var hostnameRegex = regexp.MustCompile(`^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z]{2,63})+$`)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    raw := r.URL.Query().Get("url")
    if raw == "" || len(raw) > 2083 {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    parsed, err := url.Parse(raw)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        http.Error(w, "unsupported scheme", http.StatusBadRequest)
        return
    }

    host := parsed.Hostname()
    if host == "" {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    if ip := net.ParseIP(host); ip == nil {
        if host != "localhost" && !hostnameRegex.MatchString(host) {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
    }

    port := parsed.Port()
    if port == "" {
        if parsed.Scheme == "https" {
            port = "443"
        } else {
            port = "80"
        }
    }

    addr := net.JoinHostPort(host, port)
    conn, err := net.DialTimeout("tcp", addr, 3*time.Second)
    if err != nil {
        w.Header().Set("Content-Type", "application/json")
        w.WriteHeader(http.StatusBadGateway)
        _ = json.NewEncoder(w).Encode(struct {
            Alive bool `json:"alive"`
        }{Alive: false})
        return
    }
    _ = conn.Close()

    w.Header().Set("Content-Type", "application/json")
    _ = json.NewEncoder(w).Encode(struct {
        Alive bool `json:"alive"`
    }{Alive: true})
}
