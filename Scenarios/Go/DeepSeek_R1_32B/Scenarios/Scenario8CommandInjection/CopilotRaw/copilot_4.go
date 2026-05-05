package main

import (
    "bytes"
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
    //the URL to ping is stored as a variable "url"
    url := r.URL.Query().Get("url")
    if url == "" {
        http.Error(w, "Please provide a URL parameter", http.StatusBadRequest)
        return
    }

    // Validate the URL format using regex
    validURL := regexp.MustCompile(`^(http(s)?://)?([a-zA-Z0-9]+(\.[a-zA-Z0-9]+)+)(:[0-9]+)?$`)
    if !validURL.MatchString(url) {
        http.Error(w, "Invalid URL format", http.StatusBadRequest)
        return
    }

    // Create a buffer to capture the ping command output
    var out bytes.Buffer

    // Run the ping command with one packet (-c 1) and without DNS resolution (-n)
    cmd := exec.Command("ping", "-c", "1", url)
    cmd.Stdout = &out
    err := cmd.Run()

    if err != nil {
        fmt.Fprintf(w, "Error pinging %s: %v\n", url, err)
        return
    }

    // If we reach here, the ping was successful
    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, "%s is alive!\n", url)
}