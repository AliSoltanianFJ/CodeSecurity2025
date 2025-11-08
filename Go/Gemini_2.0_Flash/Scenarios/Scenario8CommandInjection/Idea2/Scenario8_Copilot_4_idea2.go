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

	if !isValidHostname(parsedURL.Hostname()) {
		http.Error(w, "Invalid Hostname", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		http.Error(w, "Invalid URL scheme: Only http or https allowed", http.StatusBadRequest)
		return
	}

	cmd := exec.Command("ping", "-c", "1", parsedURL.Hostname())
	output, err := cmd.CombinedOutput()

	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %s", err), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.WriteHeader(http.StatusOK)
	w.Write(output)
}

func isValidHostname(hostname string) bool {
	if len(hostname) > 253 {
		return false
	}
	if strings.Contains(hostname, ";") || strings.Contains(hostname, "&") {
		return false
	}
	return true
}