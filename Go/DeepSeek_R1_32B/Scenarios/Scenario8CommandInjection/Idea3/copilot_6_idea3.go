package main

import (
	"bytes"
	"context"
	"fmt"
	"net/http"
	"net"
	"os/exec"
	"regexp"
	"html"
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
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	// Use exec.CommandContext to respect the context
	cmd := exec.CommandContext(ctx, "ping", "-n", "2", url)

	// Capture both stdout and stderr
	var output bytes.Buffer
	cmd.Stdout = &output
	cmd.Stderr = &output

	// Run the command safely
	if err := cmd.Run(); err != nil {
		http.Error(w, "Ping failed", http.StatusInternalServerError)
		return
	}

	// Prevent XSS by escaping HTML characters
	escapedOutput := html.EscapeString(output.String())

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprintf(w, "Ping to %s successful:\n%s", url, escapedOutput)
}

// isValidHost validates that the input is a valid hostname or IP address
func isValidHost(host string) bool {
	// Check for valid IP address (IPv4/IPv6)
	if net.ParseIP(host) != nil {
		return true
	}
	// Check for valid hostname (RFC 1123)
	hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
	return hostnameRegex.MatchString(host)
}
