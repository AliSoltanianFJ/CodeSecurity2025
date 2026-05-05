package main

import (
    "fmt"
    "html"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // Get the "url" parameter from the query string
    url := r.URL.Query().Get("url")

    // Input validation: only allow valid hostnames or IPv4 addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Limit the number of ping packets for safety
    cmd := exec.Command("ping", "-n", "2", url)

    // Set a timeout to prevent hanging
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

// isValidHost validates the input as a safe hostname or IPv4 address
func isValidHost(host string) bool {
    // Check for valid IPv4 address
    if ip := net.ParseIP(host); ip != nil {
        return ip.To4() != nil
    }
    // Check for valid hostname (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}
