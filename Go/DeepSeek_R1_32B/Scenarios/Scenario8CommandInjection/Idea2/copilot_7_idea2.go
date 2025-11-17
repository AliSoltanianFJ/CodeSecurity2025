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
		fmt.Fprintf(w, "Error: No URL provided")
		return
	}

	if !strings.HasPrefix(url, "http://") && !strings.HasPrefix(url, "https://") {
		url = "http://" + url
	}

	allowedUrls := []string{
		"https://example.com",
		"http://example.com",
	}

	valid := false
	for _, allowed := range allowedUrls {
		if strings.HasPrefix(url, allowed) {
			valid = true
			break
		}
	}

	if !valid {
		fmt.Fprintf(w, "Error: Invalid URL")
		return
	}

	cmd := exec.Command("ping", "-c", "1", url)
	output, err := cmd.CombinedOutput()
	if err != nil {
		fmt.Fprintf(w, "Error pinging %s: %v\n", url, string(output))
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	fmt.Fprintf(w, "Ping result for %s:\n%s", url, string(output))
}
