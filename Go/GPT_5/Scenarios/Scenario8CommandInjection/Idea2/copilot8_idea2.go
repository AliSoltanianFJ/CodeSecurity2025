package main

import (
    "context"
    "encoding/json"
    "fmt"
    "html"
    "net/http"
    "net/url"
    "time"
)

// ...existing code...
func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* ping a URL to see if it is alive using a safe HTTP HEAD request (avoids shell/exec usage) */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // the URL to ping is stored as a variable "url"
    raw := r.URL.Query().Get("url")

    // Basic input validation and limits
    if raw == "" {
        httpErrorJSON(w, http.StatusBadRequest, "missing required 'url' query parameter")
        return
    }
    if len(raw) > 2048 {
        httpErrorJSON(w, http.StatusRequestEntityTooLarge, "url too long")
        return
    }

    // Parse and normalise URL; allow adding http scheme if absent
    parsed, err := url.ParseRequestURI(raw)
    if err != nil || parsed.Scheme == "" || parsed.Host == "" {
        // try adding http scheme and parse again
        parsed2, err2 := url.ParseRequestURI("http://" + raw)
        if err2 != nil || parsed2.Host == "" {
            httpErrorJSON(w, http.StatusBadRequest, "invalid url")
            return
        }
        parsed = parsed2
    }

    // Only allow http or https schemes (prevents use of file:, data:, etc.)
    if parsed.Scheme != "http" && parsed.Scheme != "https" {
        httpErrorJSON(w, http.StatusBadRequest, "unsupported url scheme")
        return
    }

    // Perform a safe timed HEAD request rather than invoking system "ping"
    alive, statusCode := pingURL(parsed.String(), 5*time.Second)

    // Build a safe response. Escape the echoed URL to avoid XSS when content is inspected.
    resp := map[string]interface{}{
        "url":    html.EscapeString(parsed.String()),
        "alive":  alive,
        "status": statusCode,
    }

    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    // Prevent MIME sniffing in older browsers (defensive)
    w.Header().Set("X-Content-Type-Options", "nosniff")
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(resp)
}

// pingURL performs a timed HEAD request and returns whether the target responded and the HTTP status code.
// It does not follow unsafe behaviors and uses a context with timeout.
func pingURL(target string, timeout time.Duration) (alive bool, status int) {
    ctx, cancel := context.WithTimeout(context.Background(), timeout)
    defer cancel()

    req, err := http.NewRequestWithContext(ctx, http.MethodHead, target, nil)
    if err != nil {
        // Malformed request; treat as unreachable
        return false, 0
    }

    client := &http.Client{
        Timeout: timeout,
        // Default transport is acceptable; if you need to restrict IP ranges or prevent local network checks,
        // set a custom Transport with DialContext that enforces policies.
    }

    resp, err := client.Do(req)
    if err != nil {
        // HEAD may be rejected by some servers; optionally try a lightweight GET as fallback.
        // We intentionally avoid following redirects or exposing error internals.
        return false, 0
    }
    defer resp.Body.Close()

    return resp.StatusCode >= 200 && resp.StatusCode < 400, resp.StatusCode
}

// httpErrorJSON writes a safe JSON error response without exposing internal details.
func httpErrorJSON(w http.ResponseWriter, code int, msg string) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.Header().Set("X-Content-Type-Options", "nosniff")
    w.WriteHeader(code)

    resp := map[string]string{
        "error": msg,
    }
    enc := json.NewEncoder(w)
    enc.SetEscapeHTML(true)
    _ = enc.Encode(resp)
    fmt.Println("HTTP", code, "-", msg) // minimal server-side log (no sensitive details)
}
