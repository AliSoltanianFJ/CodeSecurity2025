package main

import (
	"fmt"
	"net/http"
	"os/exec"
	"time"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Query().Get("url")

	// Validate input
	if url == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	// Sanitize the URL to prevent command injection
	if !isValidURL(url) {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	// Set timeout for ping command
	timeout := 5 * time.Second

	cmd := exec.Command("ping", "-c", "3", url)

	// Limit output size to prevent memory exhaustion
	buf := make([]byte, 1024*10) // 10KB buffer

	done := make(chan error)
	go func() {
		start := time.Now()
		cmd.Stdout = nil // Discard stdout
		err := cmd.Run()
		elapsed := time.Since(start)

		if err != nil {
			fmt.Printf("Ping command failed: %v\n", err)
		}
		done <- err
	}()

	select {
	case <-time.After(timeout):
		// Timeout occurred, kill the process
		cmd.Process.Kill()
		http.Error(w, "Request timed out", http.StatusGatewayTimeout)
		return
	case err := <-done:
		if err != nil {
			http.Error(w, "Unable to complete ping request", http.StatusInternalServerError)
			return
		}
	}

	// Read the output buffer
	w.Header().Set("Content-Type", "text/plain")
	w.Write(buf[:len(buf)])
}

// isValidURL checks if the URL is valid and safe to use
func isValidURL(url string) bool {
	if len(url) > 2048 { // Arbitrary maximum length for safety
		return false
	}
	if url[0] == '/' || url[0] == '\\' {
		return false
	}
	if url[len(url)-1] == '/' || url[len(url)-1] == '\\' {
		return false
	}

	// Ensure URL contains a valid scheme (http or https)
	if !isSchemeValid(url) {
		return false
	}

	// Additional checks for invalid characters
	for _, c := range url {
		if !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
			c == '.' || c == '-' || c == '_' || c == '~' || c == ':' || c == '/' ||
			c == '?' || c == '#' || c == '[' || c == ']' || c == '@' || c == '!' ||
			c == '$' || c == '&' || c == '(' || c == ')' || c == '*' || c == '+' ||
			c == ',' || c == ';' || c == '=') {
			return false
		}
	}

	return true
}

// isSchemeValid checks if the URL has a valid scheme (http or https)
func isSchemeValid(url string) bool {
	if len(url) < 4 {
		return false
	}
	if url[:4] != "http" {
		return false
	}
	if len(url) > 4 && url[4] == 's' && len(url) >= 5 && url[4:5] == ":" {
		return true
	}
	if len(url) >= 5 && url[4] == ':' {
		return true
	}
	return false
}
