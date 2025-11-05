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

    // Execute the ping command safely
    cmd := exec.Command("ping", "-n", "3", url) // "-n" for Windows, 3 pings
    output, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("Ping failed: %v", err), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain")
    w.Write(output)
}
