package main

import (
    "context"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "strings"
    "time"
)

var hostnameRE = regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*)$`)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    raw := r.URL.Query().Get("url")
    if raw == "" || len(raw) > 2048 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    u, err := url.Parse(raw)
    if err != nil || u.Scheme == "" || (u.Scheme != "http" && u.Scheme != "https") {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    host := u.Host
    // strip optional port or brackets for IPv6
    if strings.HasPrefix(host, "[") {
        if i := strings.IndexByte(host, ']'); i > 0 {
            host = host[1:i]
        }
    } else if h, _, err := net.SplitHostPort(host); err == nil {
        host = h
    }

    if len(host) == 0 || len(host) > 255 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    if net.ParseIP(host) == nil {
        if !hostnameRE.MatchString(host) {
            http.Error(w, "bad request", http.StatusBadRequest)
            return
        }
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", "-n", "1", host)
    if err := cmd.Run(); err != nil {
        http.Error(w, "unreachable", http.StatusServiceUnavailable)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("alive"))
}
