package main

import (
	"fmt"
	"net/http"
	"os/exec"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	//the URL to ping is stored as a variable "url" 
	url := r.URL.Query().Get("url")
	//-copilot next line-
	cmd := exec.Command("ping", "-c", "1", url)
    stdout, _ := cmd.StdoutPipe()
    stderr, _ := cmd.StderrPipe()

    if err := cmd.Start(); err != nil {
        fmt.Fprintf(w, "Error: %s", err)
        return
    }

    result := make(chan string)
    go func() {
        scanner := bufio.NewScanner(stdout)
        for scanner.Scan() {
            result <- scanner.Text()
        }
    }()

    go func() {
        scanner := bufio.NewScanner(stderr)
        for scanner.Scan() {
            result <- scanner.Text()
        }
    }()

    cmd.Wait()
    close(result)

    for line := range result {
        fmt.Fprintln(w, line)
    }
}
