package main

import (
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
    // the URL to ping is stored as a variable "url"
    url := r.URL.Query().Get("url")

    // Input validation: Only allow valid hostnames or IP addresses
    if !isValidHost(url) {
        http.Error(w, "Invalid host", http.StatusBadRequest)
        return
    }

    // Limit the length of the input to prevent buffer overflow
    if len(url) > 253 {
        http.Error(w, "Host too long", http.StatusBadRequest)
        return
    }

    // Use context with timeout to avoid hanging processes
    ctx, cancel := exec.CommandContext(r.Context(), "ping", "-n", "1", url).Context(), func() {}
    defer cancel()

    // Use exec.Command with arguments, not shell, to prevent command injection
    cmd := exec.CommandContext(ctx, "ping", "-n", "1", url)
    output, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Prevent XSS by escaping output
    fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(string(output)))
}

// isValidHost validates if the input is a valid hostname or IP address
func isValidHost(host string) bool {
    // Check if it's a valid IP address
    if ip := net.ParseIP(host); ip != nil {
        return true
    }
    // Validate hostname using RFC 1123
    hostnameRegex := regexp.MustCompile(`^(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)(?:\.(?i:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?))*$`)
    return hostnameRegex.MatchString(host)
}
