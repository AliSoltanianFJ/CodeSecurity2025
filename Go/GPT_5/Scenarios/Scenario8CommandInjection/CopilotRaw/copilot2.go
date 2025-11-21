// ...existing code...
package main

import (
    "context"
    "fmt"
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
    //the URL to ping is stored as a variable "url" 
    url := r.URL.Query().Get("url")

    // validate input to avoid command injection: only allow letters, digits, dots and hyphens
    if url == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }
    if len(url) > 253 {
        http.Error(w, "url parameter too long", http.StatusBadRequest)
        return
    }
    validHost := regexp.MustCompile(`^[A-Za-z0-9\.-]+$`)
    if !validHost.MatchString(url) {
        http.Error(w, "invalid url parameter", http.StatusBadRequest)
        return
    }

    // run ping without a shell, with a timeout (Windows uses -n)
    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    cmd := exec.CommandContext(ctx, "ping", "-n", "4", url)
    out, err := cmd.CombinedOutput()
    if err != nil {
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}
