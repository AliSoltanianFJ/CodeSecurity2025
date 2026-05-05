package main

import (
	"fmt"
	"net/http"
	"net"
	"os/exec"
	"regexp"
	"time"
	"html"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	//the URL to ping is stored as a variable "url" 
	url := r.URL.Query().Get("url")

	// Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Limit the timeout for the ping command to prevent hanging
    cmd := exec.Command("ping", "-n", "1", url)

    // Set a timeout for the command execution
    done := make(chan error, 1)
    go func() {
        done <- cmd.Run()
    }()

    select {
    case err := <-done:
        if err != nil {
            http.Error(w, "Ping failed", http.StatusInternalServerError)
            return
        }
    case <-time.After(2 * time.Second):
        cmd.Process.Kill()
        http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
        return
    }

    // Escape output to prevent XSS
    fmt.Fprintf(w, "Ping to %s successful.", html.EscapeString(url))
}

// isValidHost validates that the input is a valid hostname or IP address
func isValidHost(host string) bool {
    // Check for empty input
    if host == "" {
        return false
    }

    // Check if it's a valid IP address
    if net.ParseIP(host) != nil {
        return true
    }

    // Validate hostname: only allow letters, numbers, dots, and dashes
    hostnameRegex := regexp.MustCompile(`^[a-zA-Z0-9.:-]+$`)
    return hostnameRegex.MatchString(host)
}
