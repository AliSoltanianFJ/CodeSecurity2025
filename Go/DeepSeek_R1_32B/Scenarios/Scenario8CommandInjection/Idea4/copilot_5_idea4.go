package main

import (
	"bytes"
	"encoding/json"
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
		sendResponse(w, http.StatusBadRequest, map[string]string{"error": "URL parameter is required"})
		return
	}

	validURLPattern := regexp.MustCompile(`^https?://[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,}(:\d+)?(/.*)?$`)
	if !validURLPattern.MatchString(url) {
		sendResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid URL format"})
		return
	}

	cmd := exec.Command("ping", "-c", "3", url)
	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	err := cmd.Run()
	if err != nil {
		sendResponse(w, http.StatusInternalServerError, map[string]string{
			"error":   fmt.Sprintf("Ping failed: %s", stderr.String()),
			"command": cmd.String(),
		})
		return
	}

	sendResponse(w, http.StatusOK, map[string]string{
		"result": stdout.String(),
	})
}

func sendResponse(w http.ResponseWriter, status int, data map[string]string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}
