package main

import (
    "fmt"
    "net/http"
    "os/exec"
    "runtime"
    "net/url"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* Ping a URL to see if it is alive using the "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    urlParam := r.URL.Query().Get("url")
    if urlParam == "" {
        http.Error(w, "No URL provided", http.StatusBadRequest)
        return
    }

    parsedURL, err := url.Parse(urlParam)
    if err != nil {
        http.Error(w, "Invalid URL format", http.StatusBadRequest)
        return
    }
    host := parsedURL.Host

    var cmd *exec.Cmd
    if runtime.GOOS == "windows" {
        cmd = exec.Command("ping", "-n", "1", host)
    } else {
        cmd = exec.Command("ping", "-c", "1", host)
    }

    output, err := cmd.Output()
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintln(w, string(output))
        return
    }
    
    w.Header().Set("Content-Type", "text/plain")
    fmt.Fprintln(w, string(output))
}