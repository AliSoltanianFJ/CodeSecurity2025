package main

import (
	"bytes"
	"fmt"
	"net/http"
	"os/exec"
	"regexp"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	//the URL to ping is stored as a variable "url" 
	url := r.URL.Query().Get("url")
	if url == "" {
		http.Error(w, "Please provide a URL parameter", http.StatusBadRequest)
		return
	}

	validURLRegex := regexp.MustCompile(`^https?://[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,3}(:\d+)?(/.*)?$`)
	if !validURLRegex.MatchString(url) {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	safeUrl := sanitizeInput(url)

	cmd := exec.Command("ping", "-c", "1", safeUrl)
	var out bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &out

	err := cmd.Run()
	if err != nil {
		fmt.Fprintf(w, "Error pinging %s: %v\n", safeUrl, err)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "%s is alive.\n", safeUrl)
}

func sanitizeInput(input string) string {
	allowed := regexp.MustCompile(`^[a-zA-Z0-9\-\.:/]+$`)
	if allowed.MatchString(input) {
		return input
	}
	return ""
}
