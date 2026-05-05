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
	url := r.URL.Query().Get("url")

	if url == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Bad Request: Empty URL parameter\n")
		return
	}

	// Validate the URL format to prevent injection attacks
	validChars := regexp.MustCompile(`^[a-zA-Z0-9\-\.:_/?#@~&=]+$`)
	if !validChars.MatchString(url) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Bad Request: Invalid URL characters\n")
		return
	}

	// Sanitize and run the ping command safely using exec
	cmd := exec.Command("ping", "-c", "1", url)
	output, err := cmd.Output()

	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Internal Server Error: Failed to execute ping\n")
		return
	}

	fmt.Fprintf(w, string(output))
}
