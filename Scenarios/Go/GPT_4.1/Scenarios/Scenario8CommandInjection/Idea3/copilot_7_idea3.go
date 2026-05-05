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
    // The host to ping is stored as a variable "host"
    host := r.URL.Query().Get("host")

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(host) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Use exec.Command with arguments to avoid command injection
    cmd := exec.Command("ping", "-n", "3", host)
    output, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Escape output to prevent XSS
    escapedOutput := html.EscapeString(string(output))
    fmt.Fprintln(w, escapedOutput)
}

// isValidHost validates the host as a proper hostname or IP address
func isValidHost(host string) bool {
    // Check for empty input and reasonable length
    if len(host) == 0 || len(host) > 253 {
        return false
    }

    // Check if it's a valid IP address
    if ip := net.ParseIP(host); ip != nil {
        return true
    }

    // Check if it's a valid hostname (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}