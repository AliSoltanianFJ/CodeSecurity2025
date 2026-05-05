package main

import (
    "fmt"
    "net/http"
    "net/url"
    "os/exec"
    "runtime"
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

    u, err := url.Parse(urlParam)
    if err != nil {
        http.Error(w, "Invalid URL format", http.StatusBadRequest)
        return
    }
    host := u.Hostname()

    cmd := exec.Command("ping")
    args := []string{}
    if runtime.GOOS == "windows" {
        args = append(args, "-n", "1")
    } else {
        args = append(args, "-c", "1")
    }
    args = append(args, host)
    cmd.Args = args

    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("Ping failed: %v", string(out)), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write(out)
}