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
type scrubbedUrl string
    // The URL to ping is stored as a variable "url"
url := safeUrlScrub(r.URL.Query().Get("url"))
if url == "" {
http.Error(w, "Missing or invalid url parameter", http.StatusBadRequest)
return
    }

    // Limit the timeout for the ping command to prevent hanging
timeout := 2 * time.Second
// Use context to control command execution time
ctx, cancel := context.WithTimeout(context.Background(), timeout)
defer cancel()

// Use exec.CommandContext for safety, and escape input properly
cmd := exec.CommandContext(ctx, "ping", "-n", "1", string(url))

// Get output safely
output, err := cmd.CombinedOutput()
if ctx.Err() == context.DeadlineExceeded {
http.Error(w, "Ping timed out", http.StatusGatewayTimeout)
return
}
if err != nil {
http.Error(w, fmt.Sprintf("Ping failed: %v", err), http.StatusInternalServerError)
return
}

    // Prevent XSS by escaping output
fmt.Fprintf(w, "<pre>%s</pre>", html.EscapeString(string(output)))
}

// Clean and validate the URL input to prevent injection and command execution vulnerabilities
func safeUrlScrub(input string) scrubbedUrl {
if len(input) == 0 || len(input) > 253 {
return ""
    }
    // Only allow alphanumeric, dots, and dashes (typical for hostnames)
re := regexp.MustCompile(`^[a-zA-Z0-9.-]+$`)
if !re.MatchString(input) {
return ""
    }
    // Remove any leading/trailing dots or dashes
clean := input
clean = regexp.MustCompile(`^[.-]+|[.-]+$`).ReplaceAllString(clean, "")
return scrubbedUrl(clean)
}