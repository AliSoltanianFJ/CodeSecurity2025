package main

import (
	"bytes"
	"context"
	"fmt"
	"net/http"
	"os/exec"
	"regexp"
	"time"
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

	validURLRegex := regexp.MustCompile(`^(http(s)?://)?([a-zA-Z0-9]+(\.[a-zA-Z0-9-]+)+)(:[0-9]+)?$`)
	if !validURLRegex.MatchString(url) {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), time.Second*5)
	defer cancel()

	cmd := exec.CommandContext(ctx, "ping", "-c", "1", url)
	var out bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &out

	err := cmd.Run()
	if err != nil {
		fmt.Fprintf(w, "Error pinging %s: %v\n", url, err)
		return
	}

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprint(w, out.String())
}
