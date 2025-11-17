package main

import (
	"bytes"
	"fmt"
	"io"
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

	// Sanitize and validate the input
	if url == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error: Empty URL parameter")
		return
	}

	// Regular expression to validate URL characters (only allow letters, numbers, dots, slashes, colons, and hyphens)
	validURL := regexp.MustCompile(`^[a-zA-Z0-9/.:-]+$`)
	if !validURL.MatchString(url) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Error: Invalid characters in URL")
		return
	}

	// Use exec.Command to run the ping command safely with arguments
	cmd := exec.Command("ping", "-c", "4", url)

	// Set up pipes for stdout and stderr
	outPipe, err := cmd.StdoutPipe()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v", err)
		return
	}
	defer outPipe.Close()

	errPipe, err := cmd.StderrPipe()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v", err)
		return
	}
	defer errPipe.Close()

	// Start the command
	if err := cmd.Start(); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v", err)
		return
	}

	// Read output and errors
	outChan := make(chan string, 1)
	go func() {
		buf := new(bytes.Buffer)
		_, err := buf.ReadFrom(outPipe)
		if err != nil && err != io.EOF {
			outChan <- fmt.Sprintf("Error: %v", err)
			return
		}
		outChan <- buf.String()
	}()

	errChan := make(chan string, 1)
	go func() {
		buf := new(bytes.Buffer)
		_, err := buf.ReadFrom(errPipe)
		if err != nil && err != io.EOF {
			errChan <- fmt.Sprintf("Error: %v", err)
			return
		}
		errChan <- buf.String()
	}()

	// Wait for the command to finish and handle any errors
	done := make(chan struct{})
	go func() {
		defer close(done)
		if err := cmd.Wait(); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Command failed: %v", err)
		}
	}()

	select {
	case <-done:
		break
	case <-time.After(time.Second * 5):
		cmd.Process.Kill()
		w.WriteHeader(http.StatusTimeout)
		fmt.Fprintf(w, "Request timed out")
		return
	}

	// Read results from channels
	select {
	case result := <-outChan:
		select {
		case errResult := <-errChan:
			if errResult != "" {
				w.WriteHeader(http.StatusInternalServerError)
				fmt.Fprintf(w, "Error: %s", errResult)
				return
			}
		default:
			// No error
		}
		w.Header().Set("Content-Type", "text/plain")
		fmt.Fprintf(w, "Ping result:\n%s", result)
	case errResult := <-errChan:
		if errResult != "" {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Error: %s", errResult)
			return
		}
	}
}
