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
    // the URL to ping is stored as a variable "url"
    url := r.URL.Query().Get("url")
    if url == "" {
        http.Error(w, "Missing url parameter", http.StatusBadRequest)
        return
    }

    // Allowlist of allowed hosts (for example purposes)
    allowedHosts := map[string]bool{
        "example.com":    true,
        "google.com":     true,
        "localhost":      true,
        "127.0.0.1":      true,
        // Add more allowed hosts as needed
    }

    // Parse and validate the host
    host, err := sanitizeAndValidateHost(url, allowedHosts)
    if err != nil {
        http.Error(w, "Invalid or unauthorized host", http.StatusBadRequest)
        return
    }

    // Limit the context timeout to avoid hanging processes
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()

    // Execute ping with controlled arguments
    cmd := exec.CommandContext(ctx, "ping", "-n", "2", host)
    // Prevent command injection by not using shell and passing args directly

    // Run the command with least privileges (drop to 'nobody' if possible on Windows)
    // Note: On Windows, user privilege dropping is limited. Recommend running the service as a low-priv user.

    output, err := safeRun(cmd)
    if err != nil {
        http.Error(w, "Ping failed", http.StatusInternalServerError)
        return
    }

    // Escape output to prevent XSS
    fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(output))
}

// sanitizeAndValidateHost parses, validates, and allowlists the host
func sanitizeAndValidateHost(raw string, allowed map[string]bool) (string, error) {
    // Remove URL scheme if present
    if strings.HasPrefix(raw, "http://") || strings.HasPrefix(raw, "https://") {
        u, err := url.Parse(raw)
        if err != nil {
            return "", err
        }
        raw = u.Host
    }

    // Remove port if present
    host, _, err := net.SplitHostPort(raw)
    if err != nil {
        host = raw // If no port, use as is
    }

    // Only allow alphanumeric, dots, and dashes
    if !isValidHost(host) {
        return "", errors.New("invalid host format")
    }

    // Check against allowlist
    if !allowed[strings.ToLower(host)] {
        return "", errors.New("host not allowed")
    }

    return host, nil
}

// isValidHost checks for a safe hostname (no injection chars)
func isValidHost(host string) bool {
    for _, c := range host {
        if !(c == '.' || c == '-' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
            return false
        }
    }
    return len(host) > 0 && len(host) <= 253
}

// safeRun runs the command and captures output safely
func safeRun(cmd *exec.Cmd) (string, error) {
    var buf bytes.Buffer
    cmd.Stdout = &buf
    cmd.Stderr = &buf
    err := cmd.Run()
    if err != nil {
        return "", err
    }
    // Limit output size to prevent DoS
    out := buf.String()
    if len(out) > 4096 {
        out = out[:4096] + "...(truncated)"
    }
    return out, nil
}