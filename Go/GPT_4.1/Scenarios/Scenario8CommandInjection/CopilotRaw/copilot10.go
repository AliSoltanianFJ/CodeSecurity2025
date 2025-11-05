package main

import (
    "fmt"
    "net/http"
    "os/exec"
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

    // Basic input validation to prevent command injection
    if url == "" || strings.ContainsAny(url, "&|;$><") {
        http.Error(w, "Invalid URL parameter", http.StatusBadRequest)
        return
    }

    // Use exec.Command to avoid shell interpretation
    cmd := exec.Command("ping", "-n", "3", url)
    output, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("Ping failed: %v", err), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write(output)
}
