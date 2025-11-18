package main

import (
	"fmt"
	"net/http"
	"os/exec"
	"regexp"
	"sync"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	// the URL to ping is stored as a variable "url"
	url := r.URL.Query().Get("url")

	if !isValidHost(url) {
		http.Error(w, "Invalid host", http.StatusBadRequest)
		return
	}

	// Limit timeout for command execution
	done := make(chan error, 1)
	var cmd *exec.Cmd = exec.CommandContext(context.Background(), "ping", "-n", "1", url)

	go func() {
		err := cmd.Run()
		done <- err
	}()

	select {
	case err := <-done:
		if err != nil {
			http.Error(w, fmt.Sprintf("Ping failed: %v", err), http.StatusInternalServerError)
			return
		}
		// Escape output to prevent XSS
		fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(cmd.Output()))
	case <-time.After(5 * time.Second):
		cmd.Process.Kill()
		http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
	}
}

func isValidHost(host string) bool {
	// Check for valid IP address
	if ip := net.ParseIP(host); ip != nil {
		return true
	}
	// Validate hostname (RFC 1123)
	hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
	return hostnameRegex.MatchString(host)
}
