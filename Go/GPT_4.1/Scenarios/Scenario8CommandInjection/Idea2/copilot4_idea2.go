package main

import (
    "context"
    "fmt"
    "html"
    "net"
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
    // The URL to ping is stored as a variable "url"
    url := r.URL.Query().Get("url")

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Use context to prevent hanging processes
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()

    // Use exec.CommandContext with arguments, not shell, to prevent injection
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", url)
    output, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Prevent XSS by escaping output
    fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(string(output)))
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
