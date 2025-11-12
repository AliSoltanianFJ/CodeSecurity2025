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
    if url == "" {
        http.Error(w, "Missing url parameter", http.StatusBadRequest)
        return
    }

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid url parameter", http.StatusBadRequest)
        return
    }

    // Limit ping to 2 echo requests and set a timeout
    cmd := exec.Command("ping", "-n", "2", "-w", "1000", url)

    // Set a timeout to prevent hanging processes
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
    case <-time.After(3 * time.Second):
        cmd.Process.Kill()
        http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
        return
    }

    // Escape output to prevent XSS
    fmt.Fprintf(w, "Ping to %s successful.", html.EscapeString(url))
}

// isValidHost validates that the input is a valid hostname or IP address
func isValidHost(host string) bool {
    // Check if it's a valid IP address
    if net.ParseIP(host) != nil {
        return true
    }
    // Check if it's a valid hostname (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`)
    return hostnameRegex.MatchString(host)
}