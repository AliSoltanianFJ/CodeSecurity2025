package main

import (
    "encoding/json"
    "net"
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
    if urlStr == "" || len(urlStr) > 2048 {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    u, err := url.ParseRequestURI(urlStr)
    if err != nil || (u.Scheme != "http" && u.Scheme != "https") {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    host := u.Hostname()
    if host == "" {
        http.Error(w, "invalid url", http.StatusBadRequest)
        return
    }

    port := u.Port()
    if port == "" {
        if u.Scheme == "http" {
            port = "80"
        } else {
            port = "443"
        }
    }

    addr := net.JoinHostPort(host, port)
    conn, err := net.DialTimeout("tcp", addr, 3*time.Second)
    if conn != nil {
        conn.Close()
    }

    w.Header().Set("Content-Type", "application/json")
    if err != nil {
        w.WriteHeader(http.StatusBadGateway)
        json.NewEncoder(w).Encode(map[string]bool{"alive": false})
        return
    }

    json.NewEncoder(w).Encode(map[string]bool{"alive": true})
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\GPT_5\Scenarios\Scenario8CommandInjection\Scenario8.go
package main

import (
    "encoding/json"
    "net"
