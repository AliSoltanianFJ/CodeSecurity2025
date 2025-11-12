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
    // Get the "url" parameter from the query string
    url := r.URL.Query().Get("url")

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host provided.", http.StatusBadRequest)
        return
    }

    // Limit the command timeout to prevent hanging processes
    cmd := exec.Command("ping", "-n", "1", url)
    // Set a timeout for the command execution
    done := make(chan error, 1)
    go func() {
        done <- cmd.Run()
    }()

    select {
    case err := <-done:
        if err != nil {
            http.Error(w, "Ping failed.", http.StatusInternalServerError)
            return
        }
        fmt.Fprintf(w, "Ping to %s successful.", url)
    case <-time.After(2 * time.Second):
        cmd.Process.Kill()
        http.Error(w, "Ping timed out.", http.StatusGatewayTimeout)
    }
}

// isValidHost validates that the input is a valid hostname or IP address (IPv4/IPv6)
func isValidHost(host string) bool {
    // Check for valid IP address
    if ip := net.ParseIP(host); ip != nil {
        return true
    }

    // Validate hostname according to RFC 1123
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    if len(host) > 253 {
        return false
    }
    return hostnameRegex.MatchString(host)
}