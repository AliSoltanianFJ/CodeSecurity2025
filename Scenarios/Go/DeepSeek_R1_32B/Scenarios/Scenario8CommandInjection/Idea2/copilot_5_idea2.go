package main

import (
	"fmt"
	"net/http"
	"net/url"
	"os/exec"
	"time"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Query().Get("url")
	if url == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "URL parameter is required\n")
		return
	}

	// Validate and sanitize the URL input
	parsedUrl, err := url.Parse(url)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid URL format: %v\n", err)
		return
	}

	allowedSchemes := map[string]bool{
		"http":  true,
		"https": true,
	}

	if !allowedSchemes[parsedUrl.Scheme] {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Unsupported URL scheme. Only http and https are allowed\n")
		return
	}

	cmd := exec.Command("ping", "-c", "3", parsedUrl.Host)
	timeout := 5 * time.Second

	var outBuf, errBuf []byte
	done := make(chan error, 1)

	go func() {
		out, err := cmd.Output()
		if err != nil {
			errBuf = err.Bytes()
		} else {
			outBuf = out
		}
		done <- err
	}()

	select {
	case err := <-done:
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Ping failed: %v\n", err)
		} else {
			w.WriteHeader(http.StatusOK)
			fmt.Fprintf(w, string(outBuf))
		}
	case <-time.After(timeout):
		cmd.Process.Kill()
		w.WriteHeader(http.StatusRequestTimeout)
		fmt.Fprintf(w, "Ping request timed out\n")
	}
}
