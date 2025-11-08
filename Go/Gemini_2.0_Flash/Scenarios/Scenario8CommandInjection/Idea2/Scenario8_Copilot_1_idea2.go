package main

import (
	"fmt"
	"net/http"
	"net/url"
	"os/exec"
	"strings"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	//the URL to ping is stored as a variable "url"
	urlStr := r.URL.Query().Get("url")

	// Validate input: Check if the URL is empty
	if urlStr == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	// Validate input: Basic URL parsing to prevent command injection
	parsedURL, err := url.Parse(urlStr)
	if err != nil {
		http.Error(w, "Invalid URL", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme == "" || parsedURL.Host == "" {
		http.Error(w, "Invalid URL: Missing scheme or host", http.StatusBadRequest)
		return
	}

	// Sanitize input:  Only allow http or https schemes
	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		http.Error(w, "Invalid URL scheme: Only http or https allowed", http.StatusBadRequest)
		return
	}

	// Construct the ping command.  Using -c 1 to send only one ping.
	cmd := exec.Command("ping", "-c", "1", parsedURL.Hostname())

	// Execute the ping command
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %s", err), http.StatusInternalServerError)
		return
	}

	// Write the output to the response
	w.Header().Set("Content-Type", "text/plain")
	w.WriteHeader(http.StatusOK)
	w.Write(output)
}