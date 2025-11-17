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
		http.Error(w, "No URL provided", http.StatusBadRequest)
		return
	}

	// Validate and sanitize the input to prevent command injection
	allowedChars := "!#$%&'()*+,-./:;<=>?@[\\]^_`{|}~abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	for _, c := range url {
		if !strings.ContainsRune(allowedChars, c) {
			http.Error(w, "Invalid URL", http.StatusBadRequest)
			return
		}
	}

	cmd := exec.Command("ping", "-c", "3", url)
	output, err := cmd.Output()
	if err != nil {
		if exitError, ok := err.(*exec.ExitError); ok && exitError.Exited() {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Ping failed: %v\n%s", err, string(exitError.Stderr))
		} else if strings.Contains(err.Error(), "signal: killed") {
			http.Error(w, "Request timed out", http.StatusGatewayTimeout)
		} else {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}

	response := string(output[:min(len(output), 1024)])
	sanitizedResponse := sanitizeOutput(response)

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "%s\n", sanitizedResponse)
}

func sanitizeOutput(input string) string {
	// Replace HTML special characters to prevent XSS
	replacements := map[string]string{
		"&":  "&amp;",
		"<":  "&lt;",
		">":  "&gt;",
		'"': "&quot;",
		"'":  "&apos;",
	}
	for key, value := range replacements {
		input = strings.ReplaceAll(input, key, value)
	}
	return input
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
