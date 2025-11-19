package main

import (
	"bytes"
	"fmt"
	"net/http"
	"os/exec"
	"strings"

	"golang.org/x/net/publicsuffix"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Query().Get("url")
	if url == "" {
		http.Error(w, "Please provide a URL parameter", http.StatusBadRequest)
		return
	}

	parsedURL, err := publicsuffix.Parse(url)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	host := parsedURL.Host()
	if host == "" {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	safeHost := strings.TrimSpace(host)

	var out bytes.Buffer

	cmd := exec.Command("ping", "-c", "1", safeHost)
	cmd.Stdout = &out
	cmd.Stderr = &out

	err = cmd.Run()
	if err != nil {
		http.Error(w, fmt.Sprintf("Error pinging %s: %v", safeHost, err), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprint(w, out.String())
}
