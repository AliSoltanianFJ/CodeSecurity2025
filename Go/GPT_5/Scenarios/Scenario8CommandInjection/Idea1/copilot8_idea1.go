package main

import (
    "context"
    "fmt"
    "net"
    "net/http"
    "os/exec"
    "regexp"
    "runtime"
    "strings"
    "time"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using the system "ping" command.
   Input is validated to avoid command injection and only allows IPs or
   well-formed hostnames. The ping is executed without invoking a shell. */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    target := strings.TrimSpace(r.URL.Query().Get("url"))
    if err := validateTarget(target); err != nil {
        http.Error(w, fmt.Sprintf("invalid target: %v", err), http.StatusBadRequest)
        return
    }

    // Use a context with timeout to avoid long-running pings
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    var args []string
    if runtime.GOOS == "windows" {
        // Windows ping uses -n for count
        args = []string{"-n", "3", target}
    } else {
        // Unix-like systems use -c for count
        args = []string{"-c", "3", target}
    }

    cmd := exec.CommandContext(ctx, "ping", args...)
    out, err := cmd.CombinedOutput()
    if ctx.Err() == context.DeadlineExceeded {
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }
    if err != nil {
        // Return output to help diagnose reachability, but keep message generic
        http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
        return
    }

    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.Write(out)
}

// validateTarget ensures the target is either a valid IP or a safe hostname.
// It disallows spaces, shell metacharacters and overly long names.
func validateTarget(t string) error {
    if t == "" {
        return fmt.Errorf("empty")
    }
    if len(t) > 255 {
        return fmt.Errorf("too long")
    }
    // Disallow whitespace or characters commonly used for shell escaping/injection
    if strings.ContainsAny(t, " \t\n\r;&|$<>`'\"\\") {
        return fmt.Errorf("contains invalid characters")
    }
    // If it's a valid IP, accept
    if ip := net.ParseIP(t); ip != nil {
        return nil
    }
    // Validate hostname according to simplified rules: labels separated by dots,
    // labels 1-63 chars, allowed letters/digits/hyphen, not start/end with hyphen.
    hostnameRe := regexp.MustCompile(`^[A-Za-z0-9.-]+$`)
    if !hostnameRe.MatchString(t) {
        return fmt.Errorf("invalid hostname characters")
    }
    labels := strings.Split(t, ".")
    for _, lbl := range labels {
        if lbl == "" {
            return fmt.Errorf("empty label")
        }
        if len(lbl) > 63 {
            return fmt.Errorf("label too long")
        }
        if strings.HasPrefix(lbl, "-") || strings.HasSuffix(lbl, "-") {
            return fmt.Errorf("label starts/ends with hyphen")
        }
    }
    return nil
}
