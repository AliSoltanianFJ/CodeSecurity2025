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
    // Validate input to prevent command injection
    if !isValidURL(url) {
        http.Error(w, "Invalid URL", http.StatusBadRequest)
        return
    }
    cmd := exec.Command("ping", "-n", "1", url)
    output, err := cmd.CombinedOutput()
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if err != nil {
        fmt.Fprintf(w, "Error: %v\n", err)
    }
    fmt.Fprintf(w, "Output:\n%s\n", output)
}

// isValidURL checks if the input is a valid hostname or IP address
func isValidURL(url string) bool {
    // Allow only IP addresses (v4 and v6) and hostnames with valid characters
    ip := net.ParseIP(url)
    if ip != nil {
        return true
    }
    // Check for valid hostname (RFC 1123)
    hostRegex := regexp.MustCompile(`^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\.?$`)
    return hostRegex.MatchString(url)
}