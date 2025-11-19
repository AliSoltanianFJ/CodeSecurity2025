package main

import (
	"bufio"
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

	validURLPattern := regexp.MustCompile(`^(?:https?://)?[a-zA-Z0-9.-]+(?:\.[a-zA-Z]{2,6})+(?:/[^\s]*)?$`)
	if !validURLPattern.MatchString(url) {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	cmd := exec.Command("ping", "-c", "1", url)
	var stdout bytes.Buffer
	var stderr bytes.Buffer

	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	err := cmd.Run()
	if err != nil {
		fmt.Fprintf(w, "Error pinging %s: %v\n", url, err)
		return
	}

	scanner := bufio.NewScanner(&stdout)
	for scanner.Scan() {
		fmt.Fprintln(w, scanner.Text())
	}
}
