package main

import (
	"fmt"
	"net/http"
	"os/exec"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	// the URL to ping is stored as a variable "url"
	url := r.URL.Query().Get("url")
	if url == "" {
		http.Error(w, "Missing url parameter", http.StatusBadRequest)
		return
	}

	// Basic validation: allow only alphanumeric, dots, and dashes
	if !isValidHost(url) {
		http.Error(w, "Invalid url parameter", http.StatusBadRequest)
		return
	}

	// Use exec.Command to avoid command injection
	cmd := exec.Command("ping", "-n", "3", url)
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %v", err), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.Write(output)
}

// isValidHost does basic validation for hostnames (not full RFC compliance)
func isValidHost(host string) bool {
	for _, c := range host {
		if !(c == '.' || c == '-' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
			return false
		}
	}
	return true
}
