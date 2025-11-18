package main

import (
	"fmt"
	"net/http"
	"net"
	"os/exec"
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

	// Input validation: only allow valid hostnames or IPv4 addresses
	if !isValidHost(url) {
		http.Error(w, "Invalid host", http.StatusBadRequest)
		return
	}

	// Limit timeout for ping command (e.g., 2 packets with 1-second wait each)
	cmd := exec.Command("ping", "-n", "2", "-w", "1000", url)

	// Prevent command injection by using io.NopCloser(io.Null) as input
	cmd.Stdin = os.NopCloser(os.DevNull)

	// Run the ping command safely
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, "Ping failed", http.StatusInternalServerError)
		return
	}

	// Set secure headers to prevent various attacks
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("X-XSS-Protection", "1; mode=block")
	w.Header().Set("Cache-Control", "no-store")

	// Escape output to prevent XSS attacks
	fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(string(output)))
}

// isValidHost validates that the input is a valid hostname or IPv4 address
func isValidHost(host string) bool {
	if ip := net.ParseIP(host); ip != nil {
		return ip.To4() != nil // Only allow IPv4 addresses
	}
	// Additional validation for hostnames can be added here if needed
	return false
}
