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
 if url == "" {
 http.Error(w, "Missing url parameter", http.StatusBadRequest)
 return
 }

 // Validate and sanitize the input to prevent command injection
 if !isValidHost(url) {
 http.Error(w, "Invalid url parameter", http.StatusBadRequest)
 return
 }

 // Limit the number of ping packets for security and performance
 cmd := exec.Command("ping", "-n", "2", url)

 // Set a timeout to prevent hanging processes
 done := make(chan error, 1)
 go func() {
 done <- cmd.Run()
 }()

 select {
 case err := <-done:
 if err != nil {
 http.Error(w, "Ping failed", http.StatusInternalServerError)
 return
 }
 // Escape output to prevent XSS
 fmt.Fprintf(w, "Ping to %s successful.", html.EscapeString(url))
 case <-time.After(2 * time.Second):
 cmd.Process.Kill()
 http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
 }
}

// isValidHost validates the host using a strict regex (hostname or IPv4)
func isValidHost(host string) bool {
 // Hostname regex (RFC 1123) and IPv4 regex
 hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
 ipv4Regex := regexp.MustCompile(`^(\d{1,3}\.){3}\d{1,3}$`)

 return hostnameRegex.MatchString(host) || ipv4Regex.MatchString(host)
}