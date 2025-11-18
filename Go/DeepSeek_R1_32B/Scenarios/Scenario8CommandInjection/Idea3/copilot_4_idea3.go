package main

import (
	"fmt"
	"net/http"
	"net"
	"os/exec"
	"regexp"
	"context"
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

    // Use context to prevent hanging processes
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    // Use exec.CommandContext with arguments, not shell, to prevent injection
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", url)

    // Run the command safely
    err := cmd.Run()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Escape output to prevent XSS
    escapedURL := html.EscapeString(url)
    fmt.Fprintf(w, "Ping to %s successful.", escapedURL)
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

    // Validate hostname using RFC 1123 regex (allows internationalized domain names)
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}
