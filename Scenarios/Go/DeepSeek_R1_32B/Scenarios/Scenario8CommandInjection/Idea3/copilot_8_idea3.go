package main

import (
	"fmt"
	"html"
	"net/http"
	"net/url"
	"os/exec"
	"regexp"
	"time"

	"golang.org/x/net/context"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	// the URL to ping is stored as a variable "url"
	target := r.URL.Query().Get("url")

	// Input validation: only allow valid hostnames or IP addresses
	if !isValidHost(target) {
		http.Error(w, "Invalid host", http.StatusBadRequest)
		return
	}

	// Use context to prevent hanging processes
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, "ping", "-n", "1", target)
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, "Ping failed", http.StatusInternalServerError)
		return
	}

	// Prevent XSS by escaping HTML
	escapedOutput := html.EscapeString(string(output))
	fmt.Fprintf(w, "<pre>%s</pre>", escapedOutput)
}

// isValidHost validates that the input is a valid hostname or IP address.
func isValidHost(host string) bool {
	// Check for valid IP address (IPv4 or IPv6)
	if net.ParseIP(host) != nil {
		return true
	}
	// Validate hostname using RFC 1123 regex (hostnames must be 1-63 characters,
	// can contain only a-z, A-Z, 0-9, and hyphens, and cannot start or end with a hyphen)
	pattern := `^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`
	hostnameRegex := regexp.MustCompile(pattern)
	return hostnameRegex.MatchString(host) && len(host) <= 253
}
