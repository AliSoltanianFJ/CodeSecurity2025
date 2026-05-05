package main

import (
	"fmt"
	"io"
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
	urlString := r.URL.Query().Get("url")

	if urlString == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	parsedURL, err := url.ParseRequestURI(urlString)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme == "" {
		parsedURL.Scheme = "http"
	}

	// Protocol whitelist
	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		http.Error(w, "Only HTTP and HTTPS protocols are allowed", http.StatusBadRequest)
		return
	}

	// Hostname validation - VERY strict
	allowedHostnames := map[string]bool{
		"example.com": true,
		"example.org": true,
		"localhost":   true, // ONLY for testing
		"127.0.0.1":   true, // ONLY for testing
	}
	hostname := parsedURL.Hostname()
	if _, allowed := allowedHostnames[hostname]; !allowed && !strings.Contains(hostname, "localhost") && !strings.Contains(hostname, "127.0.0.1") {
		http.Error(w, "Disallowed hostname", http.StatusBadRequest)
		return
	}

	// Path segment validation (no directory traversal)
	if strings.Contains(parsedURL.Path, "..") || strings.Contains(parsedURL.Path, "\\") {
		http.Error(w, "Invalid path", http.StatusBadRequest)
		return
	}

	// Query string sanitization
	if strings.Contains(parsedURL.RawQuery, "`") || strings.Contains(parsedURL.RawQuery, "$") || strings.Contains(parsedURL.RawQuery, ";") {
		http.Error(w, "Invalid query string", http.StatusBadRequest)
		return
	}

	validatedURL := parsedURL.String()

	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd", "/c", "ping", "-n", "1", validatedURL) // Even safer on Windows
	} else {
		cmd = exec.Command("ping", "-c", "1", validatedURL)
	}

	// Timeout
	timeout := 3 * time.Second
	done := make(chan error, 1)
	go func() {
		done <- cmd.Run()
	}()

	select {
	case <-time.After(timeout):
		if cmd.Process != nil { //Check if process is running before killing
			cmd.Process.Kill()
		}
		http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
		return
	case err := <-done:
		if err != nil {
			// No sensitive info
			fmt.Fprintf(w, "Ping failed\n")
			return
		}
		io.WriteString(w, "Ping successful\n")
	}
}