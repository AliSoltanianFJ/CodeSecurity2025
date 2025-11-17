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
        fmt.Fprintf(w, "No URL provided.")
        return
    }

    // Validate input to prevent injection attacks
    matched, err := regexp.MatchString(`^[a-zA-Z0-9\-\.]+(\.[a-zA-Z]{2,})$`, url)
    if err != nil || !matched {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Invalid URL format.")
        return
    }

    cmd := exec.Command("sh", "-c", fmt.Sprintf("ping -c 1 %s", url))
    output, err := cmd.Output()
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Error pinging the URL: %v", err)
        return
    }

    fmt.Fprint(w, string(output))
}

