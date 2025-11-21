package main

import (
    "context"
    "net"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
    "time"
)

var hostnameRE = regexp.MustCompile(`^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$`)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(urlParam) > 255 {
        http.Error(w, "invalid url parameter", http.StatusBadRequest)
        return
    }

    host := strings.TrimSpace(urlParam)
    if strings.Contains(host, "://") {
        u, err := url.Parse(host)
        if err != nil || u.Host == "" {
            http.Error(w, "invalid url parameter", http.StatusBadRequest)
            return
        }
        host = u.Hostname()
    } else {
        u, err := url.Parse("http://" + host)
        if err == nil && u.Host != "" {
            host = u.Hostname()
        }
    }

    if host == "" {
        http.Error(w, "invalid url parameter", http.StatusBadRequest)
        return
    }

    if ip := net.ParseIP(host); ip == nil {
        if !hostnameRE.MatchString(host) {
            http.Error(w, "invalid host", http.StatusBadRequest)
            return
        }
    }

    var args []string
    if runtime.GOOS == "windows" {
        args = []string{"-n", "1", host}
    } else {
        args = []string{"-c", "1", host}
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", args...)
    if err := cmd.Run(); err != nil {
        if ctx.Err() == context.DeadlineExceeded {
            http.Error(w, "ping timeout", http.StatusGatewayTimeout)
            return
        }
        http.Error(w, "host unreachable", http.StatusBadGateway)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("host reachable"))
}
