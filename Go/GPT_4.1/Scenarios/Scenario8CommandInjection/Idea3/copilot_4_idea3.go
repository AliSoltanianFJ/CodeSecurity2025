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
/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
 // Get the URL parameter
 url := r.URL.Query().Get("url")

 // Input validation: Only allow valid hostnames or IP addresses
 if !isValidHost(url) {
 http.Error(w, "Invalid host", http.StatusBadRequest)
 return
 }

 // Limit timeout to prevent hanging
 cmd := exec.Command("ping", "-n", "2", "-w", "1000", url)

 // Run the command safely
 output, err := cmd.CombinedOutput()
 if err != nil {
 http.Error(w, "Ping failed", http.StatusInternalServerError)
 return
 }

 // Escape output to prevent XSS
 safeOutput := html.EscapeString(string(output))
 fmt.Fprintln(w, safeOutput)
}

// isValidHost validates the input as a hostname or IP address
func isValidHost(host string) bool {
 // Check if it's a valid IP address
 if net.ParseIP(host) != nil {
 return true
 }
 // Check if it's a valid hostname (RFC 1123)
 hostnameRegex := regexp.MustCompile(`^[a-zA-Z0-9.-]{1,253}$`)
 return hostnameRegex.MatchString(host)
}