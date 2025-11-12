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
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Parse the URL safely
    url, err := url.Parse(r.URL.Query().Get("url"))
    if err != nil || url.Host == "" {
        http.Error(w, "Invalid URL", http.StatusBadRequest)
        return
    }

    // Only allow http, https, and empty scheme for ping
    if url.Scheme != "" && url.Scheme != "http" && url.Scheme != "https" {
        http.Error(w, "Only HTTP and HTTPS schemes allowed", http.StatusBadRequest)
        return
    }

    // Use the hostname from the parsed URL, not the user-provided input directly
    host := url.Host
    if host == "" {
        http.Error(w, "Host not provided", http.StatusBadRequest)
        return
    }

    // Validate the host (only allow alphanumeric, dots, and hyphens)
    if !regexp.MustCompile(`^[a-zA-Z0-9\.\-]+$`).MatchString(host) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Construct the ping command safely using exec.Command
    cmd := exec.Command("ping", "-n", "1", host)
    out, err := cmd.Output()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    w.Write(out)
}