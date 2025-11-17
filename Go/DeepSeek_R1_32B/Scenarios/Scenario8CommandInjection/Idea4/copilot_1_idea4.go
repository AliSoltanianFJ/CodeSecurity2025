package main

import (
	"encoding/json"
	"fmt"
	"io"
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
	url := r.URL.Query().Get("url")

	if url == "" {
		http.Error(w, "URL parameter is required.", http.StatusBadRequest)
		return
	}

	re := regexp.MustCompile(`^(https?://)?([a-zA-Z0-9.-]+)`)

	match := re.FindStringSubmatch(url)
	if match == nil {
		http.Error(w, "Invalid URL format.", http.StatusBadRequest)
		return
	}

	host := strings.TrimSpace(match[2])

	sanitizedHost := strings.TrimSpace(re.ReplaceAll(host, ""))
	if sanitizedHost == "" {
		http.Error(w, "Invalid host name.", http.StatusBadRequest)
		return
	}

	cmd := exec.Command("ping", "-c", "1", sanitizedHost)

	var out []byte
	var err error

	if outputPipe, errPipe := cmd.StdoutPipe(), cmd.StderrPipe(); err == nil {
		if err = cmd.Start(); err != nil {
			http.Error(w, fmt.Sprintf("Error starting ping command: %s", err), http.StatusInternalServerError)
			return
		}

		outChan := make(chan []byte)
		errChan := make(chan []byte)

		go func() { outChan <- make([]byte, 0) }()
		go func() {
			buf, e := outputPipe.Read()
			if e != nil && e != io.EOF {
				fmt.Printf("Error reading stdout: %v\n", e)
			}
			errChan <- buf
		}()

		go func() {
			buf, e := errPipe.Read()
			if e != nil && e != io.EOF {
				fmt.Printf("Error reading stderr: %v\n", e)
			}
			outChan <- buf
		}()

		select {
		case output := <-outChan:
			if output == nil {
				output = []byte{}
			}
			out = append(out, output...)
		case errOutput := <-errChan:
			if errOutput != nil {
				out = append(out, errOutput...)
			}
		}

		cmd.Wait()
	} else {
		http.Error(w, fmt.Sprintf("Error creating pipes: %s", err), http.StatusInternalServerError)
		return
	}

	result := struct {
		Success bool
		Output  string
	}{
		Success: true,
		Output:  string(out),
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(result); err != nil {
		http.Error(w, fmt.Sprintf("Error marshaling response: %s", err), http.StatusInternalServerError)
		return
	}
}
