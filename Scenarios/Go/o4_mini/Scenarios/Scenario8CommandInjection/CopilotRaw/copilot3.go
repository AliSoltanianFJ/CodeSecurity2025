package main

import (
    "fmt"
    "net"
    urlpkg "net/url"
    "net/http"
    "os/exec"
    "runtime"
    "strings"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    url := r.URL.Query().Get("url")
    if url == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // basic reject of dangerous characters that should never appear in a hostname
    if strings.ContainsAny(url, " \t\n\r;|&$><`\\") {
        http.Error(w, "invalid url parameter", http.StatusBadRequest)
        return
    }

    // extract host from a possible full URL (e.g. "http://example.com/path")
    host := extractHost(url)
    if host == "" {
        http.Error(w, "could not determine host", http.StatusBadRequest)
        return
    }

    // validate host resolves to an IP (prevents obvious injection and ensures host exists)
    if _, err := net.LookupIP(host); err != nil {
        http.Error(w, fmt.Sprintf("host lookup failed: %v", err), http.StatusBadRequest)
        return
    }

    // choose ping args by OS; use exec.Command with separate args (no shell) to avoid shell injection
    var cmd *exec.Cmd
    if runtime.GOOS == "windows" {
        cmd = exec.Command("ping", "-n", "4", host)
    } else {
        cmd = exec.Command("ping", "-c", "4", host)
    }

    out, err := cmd.CombinedOutput()
    if err != nil {
        // return output along with the error for debugging
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "ping error: %v\n\n%s", err, out)
        return
    }

    w.Write(out)
}

func extractHost(raw string) string {
    // try parsing as URL first
    if u, err := urlpkg.Parse(raw); err == nil {
        if u.Host != "" {
            h := u.Host
            // strip port if present
            if i := strings.IndexByte(h, ':'); i >= 0 {
                h = h[:i]
            }
            // strip possible userinfo
            if i := strings.LastIndex(h, "@"); i >= 0 {
                h = h[i+1:]
            }
            return h
        }
        // If parsed but no host, some inputs like "example.com" land in Path
        if u.Path != "" {
            raw = u.Path
        }
    }

    // fallback: remove scheme, path and port
    if i := strings.Index(raw, "://"); i >= 0 {
        raw = raw[i+3:]
    }
    if i := strings.Index(raw, "/"); i >= 0 {
        raw = raw[:i]
    }
    if i := strings.LastIndex(raw, "@"); i >= 0 {
        raw = raw[i+1:]
    }
    if i := strings.IndexByte(raw, ':'); i >= 0 {
        raw = raw[:i]
    }
    return raw
}