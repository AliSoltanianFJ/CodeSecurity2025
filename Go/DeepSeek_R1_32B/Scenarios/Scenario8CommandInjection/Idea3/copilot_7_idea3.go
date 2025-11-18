package main

import (
	"fmt"
	"net/http"
	"os/exec"
	"regexp"
	"time"

	"html"
	"context"
	"net"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	// the URL to ping is stored as a variable "url"
	url := r.URL.Query().Get("url")

	// Input validation: Only allow valid hostnames or IP addresses
	if !isValidHost(url) {
		http.Error(w, "Invalid host", http.StatusBadRequest)
		return
	}

	// Use context for timeout management
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	// Use exec.CommandContext with arguments, not shell, to prevent injection
	cmd := exec.CommandContext(ctx, "ping", "-n", "1", url)
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %v", err), http.StatusInternalServerError)
		return
	}

	// Prevent XSS by escaping output
	fmt.Fprintf(w, "Ping to %s successful:<br>%s", html.EscapeString(url), html.EscapeString(string(output)))
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

	// Validate hostname using RFC 1123 regex (allowing only alphanumerics, dots, and hyphens)
	hostnameRegex := regexp.MustCompile(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`)
	return hostnameRegex.MatchString(host)
}
