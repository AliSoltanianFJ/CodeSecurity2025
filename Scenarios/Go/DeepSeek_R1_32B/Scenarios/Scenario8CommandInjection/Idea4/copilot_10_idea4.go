package main

import (
	"bytes"
	"fmt"
	"log"
	"net/http"
	"net/url"
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

	// Validate and sanitize the input
	if url == "" {
		http.Error(w, "No URL provided", http.StatusBadRequest)
		return
	}

	// Input validation: Check if URL is in a valid format
	parsedURL, err := validateAndSanitizeURL(url)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	// Use a context with timeout to prevent DoS attacks
	ctx, cancel := r.Context(), 2*time.Second
	defer cancel()

	// Use exec.CommandContext for better resource management
	cmd := exec.CommandContext(ctx, "ping", "-c", "1", parsedURL.Host)
	var stdout, stderr bytes.Buffer

	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	startTime := time.Now()
	err = cmd.Run()
	executionTime := time.Since(startTime)

	if err != nil {
		log.Printf("Error pinging URL.")
		http.Error(w, "Failed to ping the URL", http.StatusInternalServerError)
		return
	}

	response := fmt.Sprintf("Pinged %s in %v\n%s", parsedURL.Host, executionTime, stdout.String())

	if len(response) > 1024 {
		response = response[:1024]
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Ping result:\n%s", response)
}

// validateAndSanitizeURL ensures the URL is in a correct format and extracts only the necessary parts
func validateAndSanitizeURL(rawURL string) (*url.URL, error) {
	parsed, err := url.ParseRequestURI(rawURL)
	if err != nil {
		return nil, fmt.Errorf("invalid URL: %v", err)
	}

	if parsed.Host == "" {
		return nil, fmt.Errorf("missing host in URL")
	}

	sanitized := &url.URL{
		Scheme: parsed.Scheme,
		Host:   parsed.Host,
		Path:   parsed.Path,
	}

	return sanitized, nil
}
