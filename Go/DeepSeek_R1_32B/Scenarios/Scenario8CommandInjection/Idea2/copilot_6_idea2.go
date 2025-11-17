package main

import (
	"fmt"
	"net/http"
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
		fmt.Fprintf(w, "Error: no URL provided. Please include a 'url' parameter.")
		return
	}

	// Validate and sanitize the input to prevent command injection or buffer overflow
	allowedChars := "!#$%&'*+,-./0123456789:=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~"
	for _, c := range url {
		if !stringContains(allowedChars, string(c)) {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprintf(w, "Error: invalid characters in URL.")
			return
		}
	}

	cmd := exec.Command("ping", "-c", "1", url)

	// Set a timeout to prevent long-running processes
	cmd.Timeout = 5 * time.Second

	// Create pipes for stdout and stderr
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v\n", err)
		return
	}

	stderr, err := cmd.StderrPipe()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v\n", err)
		return
	}

	// Start the command
	if err := cmd.Start(); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Error: %v\n", err)
		return
	}

	// Read output from stdout and stderr simultaneously
	go func() {
		buf := make([]byte, 1024)
		for {
			n, err := stdout.Read(buf)
			if n > 0 {
				w.Write(buf[:n])
			}
			if err != nil {
				break
			}
		}
	}()

	go func() {
		buf := make([]byte, 1024)
		for {
			n, err := stderr.Read(buf)
			if n > 0 {
				w.Write(buf[:n])
			}
			if err != nil {
				break
			}
		}
	}()

	// Wait for the command to finish
	if err := cmd.Wait(); err != nil {
		fmt.Fprintf(w, "Error: %v\n", err)
		return
	}
}

func stringContains(s string, c string) bool {
	for i := 0; i < len(s); i++ {
		if s[i] == c[0] {
			return true
		}
	}
	return false
}
