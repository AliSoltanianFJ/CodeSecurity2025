package main

import (
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
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "No URL provided. Please provide a valid URL.")
		return
	}

	// Validate the URL format to prevent injection attacks
	validURLPattern := regexp.MustCompile(`^[a-zA-Z0-9\-\.~_%/?:@&=+$,#]+$`)
	if !validURLPattern.MatchString(url) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid URL. Please provide a valid and safe URL.")
		return
	}

	// Sanitize the input to ensure only allowed characters are used
	sanitizedURL := regexp.MustCompile(`[^a-zA-Z0-9\-\.~_%/?:@&=+$,#]`).ReplaceAllString(url, "")

	// Use exec.Command with arguments to prevent shell injection
	cmd := exec.Command("ping", "-c", "3", sanitizedURL)

	output, err := cmd.CombinedOutput()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error pinging the URL: %v\n%s", err, output)
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Ping result:\n%s", output)
}
