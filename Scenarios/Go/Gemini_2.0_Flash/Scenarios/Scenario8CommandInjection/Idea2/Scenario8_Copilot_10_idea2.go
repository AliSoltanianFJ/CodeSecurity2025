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
	urlStr := r.URL.Query().Get("url")

	if urlStr == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	parsedURL, err := url.Parse(urlStr)
	if err != nil {
		http.Error(w, "Invalid URL", http.StatusBadRequest)
		return
	}

	if !isValidURL(parsedURL) {
		http.Error(w, "Invalid URL", http.StatusBadRequest)
		return
	}

	hostname := parsedURL.Hostname()

	// Validate TLD (Top-Level Domain)
	if !isValidTLD(hostname) {
		http.Error(w, "Invalid URL: Unrecognized TLD", http.StatusBadRequest)
		return
	}

	cmd := exec.Command("ping", "-c", "1", hostname)
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %s", err), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.WriteHeader(http.StatusOK)
	w.Write(output)
}

func isValidURL(u *url.URL) bool {
	if u.Scheme != "http" && u.Scheme != "https" {
		return false
	}
	if u.Host == "" {
		return false
	}
	return true
}

// Dummy TLD check - replace with a real implementation
func isValidTLD(hostname string) bool {
	validTLDs := []string{".com", ".net", ".org"} //expand this list
	for _, tld := range validTLDs {
		if strings.HasSuffix(hostname, tld) {
			return true
		}
	}
	return false
}