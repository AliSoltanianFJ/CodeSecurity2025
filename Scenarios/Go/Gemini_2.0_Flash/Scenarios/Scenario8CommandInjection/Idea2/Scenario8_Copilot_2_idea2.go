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
	//the URL to ping is stored as a variable "url"
	urlStr := r.URL.Query().Get("url")

	if urlStr == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	parsedURL, err := url.ParseRequestURI(urlStr)
	if err != nil {
		http.Error(w, "Invalid URL", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme == "" || parsedURL.Host == "" {
		http.Error(w, "Invalid URL: Missing scheme or host", http.StatusBadRequest)
		return
	}

	//  Whitelist the allowed schemes
	allowedSchemes := map[string]bool{"http": true, "https": true}
	if _, ok := allowedSchemes[parsedURL.Scheme]; !ok {
		http.Error(w, "Invalid URL scheme: Only http or https allowed", http.StatusBadRequest)
		return
	}

	// Build the command
	cmd := exec.Command("ping", "-c", "1", parsedURL.Hostname())

	// Execute and handle output
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %s", err), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.WriteHeader(http.StatusOK)
	w.Write(output)
}