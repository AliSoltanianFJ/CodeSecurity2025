package main

import (
	"context"
	"encoding/json"
	"fmt"
	"html"
	"log"
	"net/http"
	"os/exec"
	"regexp"
	"time"

	"github.com/go-ping/ping"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/*
 pingHandler securely pings a URL using context for timeouts and proper host validation.
 All user input is sanitized to prevent command injection and XSS.

 The response is in JSON format with appropriate caching headers.

 Example usage:
 GET /url/ping?url=example.com

 Security considerations:
 - Never leak internal errors to clients
 - Use context for timeout handling
 - Validate hostnames using RFC 1123 regex
 - Escape all user output to prevent XSS
 - Do not use raw command output in responses
*/
func pingHandler(w http.ResponseWriter, r *http.Request) {
	// Get and validate the URL parameter
	url := r.URL.Query().Get("url")
	if err := validateHost(url); err != nil {
		http.Error(w, "Invalid host", http.StatusBadRequest)
		return
	}

	// Use context for timeout handling
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()

	// Use go-ping instead of system ping for security and reliability
	pinger, err := ping.NewPinger(url)
	if err != nil {
		http.Error(w, "Ping failed", http.StatusInternalServerError)
		return
	}
	pinger.Context = ctx

	// Set reasonable ping parameters
	pinger.Count = 3
	pinger.Timeout = time.Second

	// Perform the ping operation safely
	resultChan := make(chan error)
	go func() {
		err := pinger.Run()
		if err != nil {
			log.Printf("Ping failed: %v", err)
		}
		resultChan <- err
	}()

	select {
	case err := <-resultChan:
		if err != nil {
			http.Error(w, "Destination unreachable", http.StatusServiceUnavailable)
			return
		}
	case <-ctx.Done():
		http.Error(w, "Request timed out", http.StatusGatewayTimeout)
		return
	}

	// Safely escape output for JSON response
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-cache")
	json.NewEncoder(w).Encode(map[string]string{
		"message": html.EscapeString(fmt.Sprintf("Ping to %s successful", url)),
	})
}

// validateHost validates that the input is a valid hostname according to RFC 1123
func validateHost(host string) error {
	// Allow only alphanumeric, dots, and dashes in hostnames
	matched, _ := regexp.MatchString(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`, host)
	if !matched {
		return fmt.Errorf("invalid hostname")
	}
	return nil
}
