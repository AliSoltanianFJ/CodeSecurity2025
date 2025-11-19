package main

import (
	"bytes"
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
	if url == "" {
		http.Error(w, "Please provide a URL parameter", http.StatusBadRequest)
		return
	}

	validURLPattern := `^https?://[a-zA-Z0-9.-]+(:\d+)?$`
	validURLRegex := regexp.MustCompile(validURLPattern)
	if !validURLRegex.MatchString(url) {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	if strings.HasPrefix(url, "http://") || strings.HasPrefix(url, "https://") {
		url = strings.TrimPrefix(url, "http://")
		url = strings.TrimPrefix(url, "https://")
	}

	cmd := exec.Command("ping", "-c", "1", url)
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr

	err := cmd.Run()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error pinging %s: %v\n%s", url, err, stderr.String())
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "%s is alive:\n%s", url, out.String())
}
