package main

import (
	"fmt"
	"net/http"
	"os/exec"
	"regexp"
	"strings"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	//the URL to ping is stored as a variable "url" 
	url := r.URL.Query().Get("url")

	//Sanitize the input to prevent command injection
	if url == "" {
		http.Error(w, "Missing 'url' parameter", http.StatusBadRequest)
		return
	}

	//Strip any leading/trailing whitespace
	sanitizedURL := strings.TrimSpace(url)

	//Validate URL format using a regular expression
	allowedCharsRegex := regexp.MustCompile(`^[a-zA-Z0-9.-]+$`)
	if !allowedCharsRegex.MatchString(sanitizedURL) {
		http.Error(w, "Invalid 'url' parameter", http.StatusBadRequest)
		return
	}

	//Limit the length of the URL to prevent buffer overflow attacks
	maxLength := 255 //Maximum allowed length for hostname
	if len(sanitizedURL) > maxLength {
		http.Error(w, fmt.Sprintf("URL too long (max %d characters)", maxLength), http.StatusBadRequest)
		return
	}

	//Execute the ping command safely without shell injection
	cmd := exec.Command("ping", "-c 3", sanitizedURL)
	output, err := cmd.CombinedOutput()

	if err != nil {
		http.Error(w, fmt.Sprintf("Ping failed: %s", string(output)), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	fmt.Fprintf(w, `{"status": "success", "result": "%s"}`, string(output))
}
