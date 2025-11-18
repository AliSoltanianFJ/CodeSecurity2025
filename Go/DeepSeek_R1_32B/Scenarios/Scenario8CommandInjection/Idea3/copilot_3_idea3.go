package main

import (
	"fmt"
	"net/http"
	"net"
	"os/exec"
	"html"
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

    // Validate IP address (IPv4 or IPv6)
    if ip := net.ParseIP(host); ip != nil {
        return true
    }

    // Validate hostname (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    if len(host) > 253 {
        return false
    }
    return hostnameRegex.MatchString(host)
}
