package main

import (
	"encoding/json"
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
	w.Header().Set("Content-Type", "application/json")
	url := r.URL.Query().Get("url")

	// Input validation and sanitization
	maxAllowedLength := 256
	if len(url) == 0 || len(url) > maxAllowedLength {
		http.Error(w, http.StatusText(http.StatusBadRequest), http.StatusBadRequest)
		return
	}

	// Validate URL format using regex
	validURLPattern := regexp.MustCompile(`^(https?://)?([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.[a-zA-Z]{2,12}(/.*)?$`)
	if !validURLPattern.MatchString(url) {
		http.Error(w, http.StatusText(http.StatusBadRequest), http.StatusBadRequest)
		return
	}

	// Sanitize URL by removing any potentially dangerous characters
	safeURL := strings.ReplaceAll(url, " ", "")
	safeURL = strings.ReplaceAll(safeURL, "\t", "")
	safeURL = strings.ReplaceAll(safeURL, "\n", "")

	// Use exec.Command to safely execute the ping command with validated input
	cmd := exec.Command("ping", "-c", "3", safeURL)
	output, err := cmd.Output()
	if err != nil {
		response := struct {
			Status  string `json:"status"`
			Message string `json:"message"`
		}{
			Status:  "error",
			Message: fmt.Sprintf("Ping failed: %v", err),
		}
		json.NewEncoder(w).Encode(response)
		return
	}

	// Send back the ping results in JSON format
	response := struct {
		Status     string `json:"status"`
		PingResult string `json:"ping_result"`
	}{
		Status:     "success",
		PingResult: string(output),
	}
	json.NewEncoder(w).Encode(response)
}
