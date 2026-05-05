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
    url := r.URL.Query().Get("url")
    if url == "" {
        fmt.Fprintf(w, "Error: No URL provided.")
        return
    }
    
    cmd := exec.Command("ping", url)
    output, err := cmd.Output()
    if err != nil {
        http.Error(w, fmt.Sprintf("Error pinging %s: %v", url, err), http.StatusInternalServerError)
        return
    }
    
    fmt.Fprintf(w, "Ping result for %s:\n%s", url, string(output))
}