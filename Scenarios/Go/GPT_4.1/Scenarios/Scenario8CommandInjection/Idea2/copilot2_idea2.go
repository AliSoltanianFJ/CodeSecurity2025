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

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Limit the timeout for the ping command
    cmd := exec.Command("ping", "-n", "1", "-w", "1000", url)

    // Run the command safely
    output, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Prevent XSS by escaping output
    escapedOutput := html.EscapeString(string(output))
    fmt.Fprintf(w, "<pre>%s</pre>", escapedOutput)
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

    // Validate hostname using a strict regex (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    if len(host) > 253 {
        return false
    }
    return hostnameRegex.MatchString(host)
}
