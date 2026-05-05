package main

import (
    "context"
    "fmt"
    "net/http"
    "os/exec"
    "regexp"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    url := r.URL.Query().Get("url")
    //-copilot next line-
    if url == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Basic validation to prevent command injection: allow only hostname/IP characters
    validHost := regexp.MustCompile(`^[a-zA-Z0-9.\-:]+$`)
    if !validHost.MatchString(url) || len(url) > 255 {
        http.Error(w, "invalid url parameter", http.StatusBadRequest)
        return
    }

    // Use a context with timeout so ping cannot run indefinitely
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    // Use exec.CommandContext with arguments (no shell) to avoid shell interpretation/injection.
    // On Windows the ping flag for count is "-n".
    cmd := exec.CommandContext(ctx, "ping", "-n", "4", url)

    out, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "ping error: %v\n\n%s", err, out)
        return
    }

    w.Write(out)
}
// ...existing code...
