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
    url := r.URL.Query().Get("url")

    // Sanitize input: only allow hostname or IP address
    safeURL := strings.TrimSpace(url)
    if !isValidHost(safeURL) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Limit execution time to 3 seconds
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Use exec.CommandContext to prevent command injection
    cmd := exec.CommandContext(ctx, "ping", safeURL)
    output, err := cmd.Output()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Write output to the response
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(output)
}

// isValidHost checks if the host is a valid hostname or IP address
func isValidHost(host string) bool {
    // Hostname: letters, digits, hyphens, and dots (no spaces)
    hostnameRegex := regexp.MustCompile(`^([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+$`)
    // IP Address: dotted-decimal notation
    ipRegex := regexp.MustCompile(`^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$`)

    return hostnameRegex.MatchString(host) || ipRegex.MatchString(host)
}