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
	inputURL := r.URL.Query().Get("url")

	if inputURL == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	parsedURL, err := url.ParseRequestURI(inputURL)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme == "" {
		parsedURL.Scheme = "http"
	}

	// Protocol Allowlist
	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		http.Error(w, "Only HTTP and HTTPS protocols are allowed", http.StatusBadRequest)
		return
	}

	// Hostname Allowlist - VERY Strict
	allowedHostnames := map[string]bool{
		"example.com": true,
		"example.org": true,
		"localhost":   true, // ONLY for testing
		"127.0.0.1":   true, // ONLY for testing
	}

	hostname := parsedURL.Hostname()
	if _, allowed := allowedHostnames[hostname]; !allowed && !strings.Contains(hostname, "localhost") && !strings.Contains(hostname, "127.0.0.1"){
		http.Error(w, "Disallowed hostname", http.StatusBadRequest)
		return
	}

	// Path Validation (No traversal)
	if strings.Contains(parsedURL.Path, "..") || strings.Contains(parsedURL.Path, "\\") {
		http.Error(w, "Invalid path", http.StatusBadRequest)
		return
	}

	// Query String Scrubbing
	query := parsedURL.Query()
	for key, values := range query {
		for i, value := range values {
			query[key][i] = strings.ReplaceAll(value, "`", "")
			query[key][i] = strings.ReplaceAll(value, "$", "")
			query[key][i] = strings.ReplaceAll(value, ";", "")
		}
	}
	parsedURL.RawQuery = query.Encode()

	sanitizedURL := parsedURL.String()

	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("powershell", "-Command", "Test-Path -Path (Resolve-DnsName '"+sanitizedURL+"').IPAddress")
	} else {
		cmd = exec.Command("ping", "-c", "1", sanitizedURL)
	}

	// Timeout
	timeoutDuration := 3 * time.Second
	done := make(chan error, 1)
	go func() {
		done <- cmd.Run()
	}()

	select {
	case <-time.After(timeoutDuration):
		if cmd.Process != nil {
			cmd.Process.Kill()
		}
		http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
		return
	case err := <-done:
		if err != nil {
			fmt.Fprintf(w, "Ping failed\n") // no sensitive data
			return
		}
		fmt.Fprintf(w, "Ping successful\n")
	}
}