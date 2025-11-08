package main

import (
	"fmt"
	"net/http"
	"net/url"
	"os/exec"
	"runtime"
	"strings"
	"time"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	//the URL to ping is stored as a variable "url"
	rawURL := r.URL.Query().Get("url")

	if rawURL == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	parsedURL, err := url.ParseRequestURI(rawURL)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme == "" {
		parsedURL.Scheme = "http" // or "https", depending on your default
	}

	// Strict protocol checking to prevent SSRF.  Only allow http/https.
	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		http.Error(w, "Only HTTP and HTTPS protocols are allowed", http.StatusBadRequest)
		return
	}

	// Domain validation using allow list to prevent SSRF.
	// Replace with your actual allowed domains.  This example is intentionally strict.
	allowedDomains := map[string]bool{
		"example.com":   true,
		"example.org":   true,
		"localhost":     true, //Only for testing in a controlled environment
		"127.0.0.1":     true, //Only for testing in a controlled environment
	}

	hostname := parsedURL.Hostname()
	if _, allowed := allowedDomains[hostname]; !allowed && !strings.Contains(hostname, "localhost") && !strings.Contains(hostname, "127.0.0.1"){  //Explicit check for localhost
		http.Error(w, "Disallowed domain", http.StatusBadRequest)
		return
	}

	// Path validation - VERY strict to prevent path traversal.  Reject anything but simple paths.
	if strings.Contains(parsedURL.Path, "..") || strings.Contains(parsedURL.Path, "\\") {
		http.Error(w, "Invalid path", http.StatusBadRequest)
		return
	}

	//Query validation - reject complex queries
	if strings.Contains(parsedURL.RawQuery, "&") || strings.Contains(parsedURL.RawQuery, ";") {
		http.Error(w, "Invalid query", http.StatusBadRequest)
		return
	}

	// Construct the final URL.
	finalURL := parsedURL.String()

	var cmd *exec.Cmd

	//OS-specific command execution to prevent command injection
	if runtime.GOOS == "windows" {
		cmd = exec.Command("ping", "-n", "1", finalURL)
	} else {
		cmd = exec.Command("ping", "-c", "1", finalURL)
	}

	// Set a timeout to prevent indefinite hanging.
	var timeout = 5 * time.Second
	done := make(chan error, 1)
	go func() {
		done <- cmd.Run()
	}()

	select {
	case <-time.After(timeout):
		if err := cmd.Process.Kill(); err != nil {
			fmt.Fprintf(w, "Failed to kill process: %s\n", err.Error())
			return
		}
		http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
		return
	case err := <-done:
		if err != nil {
			fmt.Fprintf(w, "Ping failed: %s\n", err.Error()) //No sensitive info
			return
		}
		fmt.Fprint(w, "Ping successful")
	}
}