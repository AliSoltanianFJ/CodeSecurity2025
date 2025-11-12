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
    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Limit the timeout for the ping command
    cmd := exec.Command("ping", "-n", "1", "-w", "1000", url)

    // Run the command with a timeout to prevent hanging
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
        fmt.Fprintf(w, "Ping to %s successful\n", url)
    case <-time.After(2 * time.Second):
        cmd.Process.Kill()
        http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
    }
}

// isValidHost validates that the input is a valid hostname or IP address
func isValidHost(host string) bool {
    // Check for valid IP address
    if ip := net.ParseIP(host); ip != nil {
        return true
    }
    // Check for valid hostname (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`)
    return hostnameRegex.MatchString(host)
}