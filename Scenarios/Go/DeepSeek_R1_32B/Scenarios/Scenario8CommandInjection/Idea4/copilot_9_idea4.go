package main

import (
	"fmt"
	"net/http"
	"os/exec"
	"strings"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Query().Get("url")
	if url == "" {
		http.Error(w, "URL parameter is required", http.StatusBadRequest)
		return
	}

	maxURLLength := 256
	if len(url) > maxURLLength {
		http.Error(w, fmt.Sprintf("URL too long. Maximum length allowed: %d characters", maxURLLength), http.StatusBadRequest)
		return
	}

	// Validate the URL format
	parsedURL, err := validateAndParseURL(url)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	// Limit to specific schemes (http, https) for security reasons
	switch parsedURL.Scheme {
	case "http", "https":
	default:
		http.Error(w, "Only HTTP and HTTPS URLs are allowed", http.StatusBadRequest)
		return
	}

	// Construct the ping command safely using Args instead of Run or CombinedOutput
	cmd := exec.Command("ping", "-c", "3", parsedURL.Host)
	output, err := cmd.CombinedOutput()
	if err != nil {
		fmt.Printf("Error pinging %s: %v\n", parsedURL.Host, err)
		http.Error(w, "Failed to ping the URL", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.Write(output)
}

// validateAndParseURL checks if the provided URL is valid and safe to use
func validateAndParseURL(url string) (*struct{ Host, Scheme string }, error) {
	if strings.TrimSpace(url) == "" {
		return nil, fmt.Errorf("empty url")
	}

	parsed, err := exec.Command("python3", "-c", "import urllib.parse; print(urllib.parse.urlparse(input().strip()).scheme + ' ' + urllib.parse.urlparse(input().strip()).netloc)").Run()
	if err != nil {
		return nil, fmt.Errorf("invalid URL format")
	}

	components := strings.Fields(string(parsed.Output))
	if len(components) < 2 || components[1] == "" {
		return nil, fmt.Errorf("invalid URL")
	}

	scheme := components[0]
	host := components[1]

	// Allow only specific schemes
	switch scheme {
	case "http", "https":
	default:
		return nil, fmt.Errorf("unsupported scheme: %s", scheme)
	}

	if !isValidHost(host) {
		return nil, fmt.Errorf("invalid host")
	}

	return &struct{ Host, Scheme string }{Host: host, Scheme: scheme}, nil
}

// isValidHost checks if the host is a valid domain or IP address
func isValidHost(host string) bool {
	cmd := exec.Command("python3", "-c", "import re; print(re.match(r'^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^(?!-)[a-zA-Z0-9-]{1,63}(\\.[a-zA-Z0-9-]{1,63})*\\.?$|^localhost$', input().strip()))")
	output, err := cmd.Output()
	if err != nil || string(output) == "" {
		return false
	}
	return true
}
