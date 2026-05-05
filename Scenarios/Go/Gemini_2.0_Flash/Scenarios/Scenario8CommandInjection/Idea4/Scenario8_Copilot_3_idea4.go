package main

import (
	"fmt"
	"net"
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

	host := parsedURL.Hostname()

	if isPrivateIP(host) {
		http.Error(w, "Pinging private IPs is not allowed", http.StatusBadRequest)
		return
	}

	// Check if the hostname resolves to a public IP address.
	ips, err := net.LookupIP(host)
	if err != nil {
		http.Error(w, "Could not resolve host: "+err.Error(), http.StatusBadRequest)
		return
	}

	allPrivate := true
	for _, ip := range ips {
		if !isPrivateIP(ip.String()) {
			allPrivate = false
			break
		}
	}

	if allPrivate {
		http.Error(w, "Hostname resolves only to private IPs, which is not allowed", http.StatusBadRequest)
		return
	}

	// Construct the ping command.  We avoid using the shell to prevent command injection.
	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("ping", "-n", "1", host) // -n 1 is equivalent to -c 1 on Linux
	} else {
		cmd = exec.Command("ping", "-c", "1", host)
	}

	// Set a timeout to prevent indefinite hanging
	var timeout = 5 * time.Second
	done := make(chan error, 1)

	go func() {
		output, err := cmd.CombinedOutput() // Capture output in goroutine
		if err != nil {
			done <- fmt.Errorf("Ping failed: %s, Output: %s", err.Error(), string(output))
			return
		}
		done <- nil // Signal success
	}()

	select {
	case <-time.After(timeout):
		if err := cmd.Process.Kill(); err != nil {
			http.Error(w, "Failed to kill process: "+err.Error(), http.StatusInternalServerError)
			return
		}
		http.Error(w, "Ping timed out", http.StatusRequestTimeout)
		return
	case err := <-done:
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError) // Use the error from the goroutine
			return
		}

		w.WriteHeader(http.StatusOK)
		w.Header().Set("Content-Type", "text/plain")
		w.Write([]byte("Ping successful")) // Send success message
	}
}

func isPrivateIP(host string) bool {
	ip := net.ParseIP(host)
	if ip == nil {
		return false
	}

	if ip.IsLoopback() || ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() {
		return true
	}

	_, private24BitBlock, _ := net.ParseCIDR("10.0.0.0/8")
	_, private20BitBlock, _ := net.ParseCIDR("172.16.0.0/12")
	_, private16BitBlock, _ := net.ParseCIDR("192.168.0.0/16")

	return private24BitBlock.Contains(ip) || private20BitBlock.Contains(ip) || private16BitBlock.Contains(ip)
}