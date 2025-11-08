package main

import (
	"fmt"
	"net/http"
	"net/url"
	"os/exec"
	"runtime"
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
		http.Error(w, "Invalid URL: "+err.Error(), http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme == "" {
		http.Error(w, "URL scheme (http/https) is required", http.StatusBadRequest)
		return
	}

	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		http.Error(w, "Only HTTP/HTTPS URLs are allowed", http.StatusBadRequest)
		return
	}

	if strings.Contains(parsedURL.Host, ":") {
		hostParts := strings.Split(parsedURL.Host, ":")
		hostname := hostParts[0]
		if isPrivateIP(hostname) {
			http.Error(w, "Pinging private IPs is not allowed", http.StatusBadRequest)
			return
		}
	} else {
		if isPrivateIP(parsedURL.Host) {
			http.Error(w, "Pinging private IPs is not allowed", http.StatusBadRequest)
			return
		}
	}

	// Construct the ping command.  We avoid using the shell to prevent command injection.
	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("ping", "-n", "1", parsedURL.Host) // -n 1 is equivalent to -c 1 on Linux
	} else {
		cmd = exec.Command("ping", "-c", "1", parsedURL.Host)
	}

	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %s, Output: %s", err.Error(), string(output)), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "text/plain")
	w.Write(output)
}

func isPrivateIP(host string) bool {
	// Check if the host is an IP address
	ip := net.ParseIP(host)
	if ip == nil {
		return false // If not an IP, it's likely a domain, so treat as public.
	}

	if ip.IsLoopback() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
		return true
	}

	_, private24BitBlock, _ := net.ParseCIDR("10.0.0.0/8")
	_, private20BitBlock, _ := net.ParseCIDR("172.16.0.0/12")
	_, private16BitBlock, _ := net.ParseCIDR("192.168.0.0/16")

	return private24BitBlock.Contains(ip) || private20BitBlock.Contains(ip) || private16BitBlock.Contains(ip)
}