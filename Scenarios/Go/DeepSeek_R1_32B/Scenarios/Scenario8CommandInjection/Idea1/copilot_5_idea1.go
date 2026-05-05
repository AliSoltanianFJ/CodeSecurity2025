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
		http.Error(w, "Error: no URL parameter provided", http.StatusBadRequest)
		return
	}

	validURLRegex := regexp.MustCompile(`^[a-zA-Z0-9\-\.]+(\:[0-9]+)?$`)
	if !validURLRegex.MatchString(url) {
		http.Error(w, "Error: invalid URL format", http.StatusBadRequest)
		return
	}

	for _, c := range url {
		if !(c == '-' || c == '.' || c == '_' || c == ':' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
			http.Error(w, "Error: invalid URL characters", http.StatusBadRequest)
			return
		}
	}

	cmd := exec.Command("ping", "-c", "1", "-n", url)
	var out bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &out

	err := cmd.Run()
	if err != nil {
		result := out.String()

		networkUnreachableRegex := regexp.MustCompile(`Network is unreachable`)
		if networkUnreachableRegex.MatchString(result) {
			http.Error(w, "Error: Network is unreachable", http.StatusServiceUnavailable)
			return
		}

		fmt.Printf("Error pinging %s: %v\n", url, err)
		http.Error(w, "Ping failed", http.StatusInternalServerError)
		return
	}

	result := out.String()
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Result of pinging %s:\n%s", url, result)
}
