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
    // the URL to ping is stored as a variable "url"
    url := r.URL.Query().Get("url")
    if url == "" {
        http.Error(w, "Missing url parameter", http.StatusBadRequest)
        return
    }

    // Only allow alphanumeric, dot, and dash to avoid command injection
    for _, c := range url {
        if !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '.' && c != '-' {
            http.Error(w, "Invalid url parameter", http.StatusBadRequest)
            return
        }
    }

    // Use exec.Command to safely run ping
    cmd := exec.Command("ping", "-n", "3", url) // "-n" for Windows, use "-c" for Unix
    output, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("Ping failed: %v", err), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write(output)
}
