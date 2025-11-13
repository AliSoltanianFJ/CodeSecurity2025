package main

import (
    "fmt"
    "net/http"
    "os/exec"
    "regexp"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    url := r.URL.Query().Get("url")
    if url == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Please provide a URL with ?url= parameter")
        return
    }

    safeURLPattern := regexp.MustCompile(`^([a-zA-Z0-9.-]+(:[0-9]+)?)$`)
    if !safeURLPattern.MatchString(url) {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Invalid URL format. Please use a simple domain or IP address.")
        return
    }

    cmd := exec.Command("ping", "-c", "3", url)
    out, err := cmd.Output()
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Error pinging %s: %v", url, err)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    fmt.Fprintln(w, string(out))
}