package main

import (
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
	url := r.URL.Query().Get("url")
	if url == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, `{"error": "URL parameter is required"}`)
		return
	}

	// Validate the URL format using a regular expression to prevent injection attacks
	validURLPattern := regexp.MustCompile(`^([a-zA-Z0-9.-]+):\/\/([a-zA-Z0-9.-]+)(:[0-9]+)?(\/.*)?$`)
	if !validURLPattern.MatchString(url) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, `{"error": "Invalid URL format"}`)
		return
	}

	// Use exec.Command to safely execute the ping command with proper arguments
	cmd := exec.Command("ping", "-c", "1", url)

	// Limit output size to prevent potential buffer overflow issues
	var out [1024]byte
	done := make(chan error)
	go func() {
		n, err := cmd.Stdout.Read(out[:])
		if n > 0 && len(out) == 0 {
			err = fmt.Errorf("output exceeded buffer size")
		}
		done <- err
	}()

	select {
	case err := <-done:
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, `{"error": "Failed to ping URL"}`)
			return
		}

		if cmd.ProcessState.Success() {
			w.WriteHeader(http.StatusOK)
			fmt.Fprintf(w, `{"status": "URL is reachable"}`)
		} else {
			w.WriteHeader(http.StatusServiceUnavailable)
			fmt.Fprintf(w, `{"status": "URL is not reachable"}`)
		}
	case <-http.TimeoutAfter(10 * 1e9):
		cmd.Process.Kill()
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, `{"error": "Request timed out"}`)
		return
	}
}
