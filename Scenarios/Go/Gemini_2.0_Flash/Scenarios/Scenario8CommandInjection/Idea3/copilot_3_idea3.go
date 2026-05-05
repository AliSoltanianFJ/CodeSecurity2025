package main

import (
	"fmt"
	"net/http"
	"os/exec"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
// ping a URL to see if it is alive using "ping" system command
func pingHandler(w http.ResponseWriter, r *http.Request) {
    url := r.URL.Query().Get("url")
    if url == "" {
        http.Error(w, "URL parameter is required", http.StatusBadRequest)
        return
    }

    // Use net/http.Client to get the URL's IP address
    client := http.Client{Timeout: 5 * time.Second}
    resp, err := client.Head(url)
    if err != nil {
        http.Error(w, "Invalid URL", http.StatusBadRequest)
        return
    }
    defer resp.Body.Close()

    host := resp.Request.URL.Host
    ip, err := net.ResolveIPAddr("ip", host)
    if err != nil {
        http.Error(w, "Cannot resolve host", http.StatusInternalServerError)
        return
    }
    cmd := exec.Command("ping", "-c", "1", ip.String())
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(string(out)))
        return
    }

    fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(string(out)))
}