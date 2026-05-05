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
		fmt.Fprintf(w, "Error: URL parameter is required\n")
		return
	}

	// Validate input to prevent command injection
	validURLRegex := regexp.MustCompile(`^([a-zA-Z0-9\-\.]+)(\.[a-zA-Z]{2,})?(:\d{1,5})?(/.*)?$`)
	if !validURLRegex.MatchString(url) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error: Invalid URL format\n")
		return
	}

	// Sanitize the input by escaping shell metacharacters
	sanitizedUrl := regexp.MustCompile(`[;|&]`).ReplaceAllLiteralString(url, "")

	cmd := exec.Command("ping", "-c", "4", sanitizedUrl)
	stdoutPipe, err := cmd.StdoutPipe()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v\n", err)
		return
	}
	defer stdoutPipe.Close()

	stderrPipe, err := cmd.StderrPipe()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v\n", err)
		return
	}
	defer stderrPipe.Close()

	if err := cmd.Start(); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v\n", err)
		return
	}

	go func() {
		cmd.Wait()
	}()

	output := make(chan string)
	go func() {
		var stdoutBuf, stderrBuf []byte
		if stdoutPipe != nil {
			stdoutBuf, _ = cmd.Output()
		}
		if stderrPipe != nil {
			stderrBuf, _ = cmd.StderrPipe().ReadAll()
		}
		output <- string(stdoutBuf) + string(stderrBuf)
	}()

	select {
	case res := <-output:
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "Ping result: %s\n", res)
	case <-cmd.Process.Exited:
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: Ping failed\n")
	}
}
