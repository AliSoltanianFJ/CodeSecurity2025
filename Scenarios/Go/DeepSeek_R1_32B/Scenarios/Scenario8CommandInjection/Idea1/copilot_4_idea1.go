package main

import (
	"bytes"
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
	urlToPing := r.URL.Query().Get("url")
	if urlToPing == "" {
		http.Error(w, "Please provide a URL parameter", http.StatusBadRequest)
		return
	}

	parsedUrl, err := parseURL(urlToPing)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	sanitizedHost := sanitizeHost(parsedUrl.Host)

	var out bytes.Buffer

	cmd := exec.Command("ping", "-c", "1", "-n", "-6", sanitizedHost)
	cmd.Stdout = &out
	err = cmd.Run()

	if err != nil {
		fmt.Fprintf(w, "Error pinging %s: %v\n", urlToPing, err)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "%s is alive!\n", urlToPing)
}

func parseURL(urlStr string) (*url.URL, error) {
	if strings.HasPrefix(urlStr, "http://") || strings.HasPrefix(urlStr, "https://") {
		return url.Parse(urlStr)
	}
	return url.Parse("http://" + urlStr)
}

func sanitizeHost(host string) string {
	host = strings.TrimSpace(host)
	host = strings.ReplaceAll(host, ";", "")
	host = strings.ReplaceAll(host, "&", "")
	host = strings.ReplaceAll(host, "|", "")
	return host
}
