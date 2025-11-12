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

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Context with timeout to avoid hanging processes
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Use exec.CommandContext to avoid command injection
    cmd := exec.CommandContext(ctx, "ping", "-n", "2", url)

    // Get only the output we need
    output, err := cmd.Output()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Escape output to prevent XSS
    safeOutput := html.EscapeString(string(output))
    fmt.Fprintf(w, "<pre>%s</pre>", safeOutput)
}

// isValidHost validates that the input is a valid hostname or IP address
func isValidHost(host string) bool {
    // Check for valid IP address
    if ip := net.ParseIP(host); ip != nil {
        return true
    }
    // Check for valid hostname (RFC 1123)
    hostnameRegex := regexp.MustCompile(`^([a-zA-Z0-9-]{1,63}\.)*[a-zA-Z0-9-]{1,63}$`)
    return hostnameRegex.MatchString(host)
}