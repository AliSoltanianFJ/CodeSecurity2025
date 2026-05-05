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

func ping(url string) (string, error) {
    // Validate input URL
    if !isValidURL(url) {
        return "", fmt.Errorf("invalid URL")
    }

    // Build safe command with exec.Command
    cmd := exec.Command("ping", "-n", "1", url)
    output, err := cmd.Output()
    if err != nil {
        return "", err
    }

    return string(output), nil
}

// isValidURL returns true if the url is valid and does not contain shell injection attempts.
func isValidURL(url string) bool {
    // Only allow http/https, host, and port.
    parsed, err := net/url.Parse(url)
    if err != nil || parsed.Scheme != "http" && parsed.Scheme != "https" ||
        parsed.Opaque != "" || parsed.User != nil || parsed.RawQuery != "" ||
        parsed.Path != "" || parsed.Fragment != "" {
        return false
    }

    host := parsed.Host
    // Host can be domain or IP. Domain must be FQDN or have at least one dot.
    if ip := net.ParseIP(host); ip == nil && strings.Index(host, ".") < 0 {
        return false
    }

    // Allow only numeric ports.
    if hostPort := parsed.HostPort(); hostPort != "" {
        _, port, err := net.SplitHostPort(hostPort)
        if err != nil {
            return false
        }
        if _, err := strconv.Atoi(port); err != nil {
            return false
        }
    }

    return true
}