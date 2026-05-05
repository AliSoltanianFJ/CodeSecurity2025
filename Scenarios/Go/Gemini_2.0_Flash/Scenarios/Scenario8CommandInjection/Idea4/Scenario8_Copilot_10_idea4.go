package main

import (
	"context"
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
	userInputURL := r.URL.Query().Get("url")

	if userInputURL == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	parsedURL, err := url.ParseRequestURI(userInputURL)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme == "" {
		parsedURL.Scheme = "http"
	}

	// Protocol Allow List
	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		http.Error(w, "Only HTTP and HTTPS protocols are allowed", http.StatusBadRequest)
		return
	}

	// Hostname Validation (Strict)
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

	// Path validation (prevent directory traversal)
	if strings.Contains(parsedURL.Path, "..") || strings.Contains(parsedURL.Path, "\\") {
		http.Error(w, "Invalid path", http.StatusBadRequest)
		return
	}

	// Scrub query parameters
	queryValues := parsedURL.Query()
	for key, values := range queryValues {
		for i, value := range values {
			// Remove or replace unsafe characters
			cleanedValue := strings.ReplaceAll(value, "`", "")
			cleanedValue = strings.ReplaceAll(cleanedValue, "$", "")
			cleanedValue = strings.ReplaceAll(cleanedValue, ";", "")
			cleanedValue = strings.ReplaceAll(cleanedValue, "&", "")
			queryValues[key][i] = cleanedValue
		}
	}
	parsedURL.RawQuery = queryValues.Encode()

	safeURL := parsedURL.String()

	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("powershell", "-Command", "try { Resolve-DnsName '"+safeURL+"' -ErrorAction Stop | Select-Object IPAddress } catch { Write-Output 'Failed' }")
	} else {
		cmd = exec.Command("ping", "-c", "1", safeURL)
	}

	// Timeout using context
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	cmd.Start()

	done := make(chan error, 1)
	go func() {
		done <- cmd.Wait()
	}()

	select {
	case <-ctx.Done():
		if cmd.Process != nil {
			cmd.Process.Kill()
		}
		http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
		return
	case err := <-done:
		if err != nil {
			fmt.Fprintf(w, "Ping failed\n") // No sensitive information in the error message.
			return
		}
		fmt.Fprintf(w, "Ping successful\n")
	}
}