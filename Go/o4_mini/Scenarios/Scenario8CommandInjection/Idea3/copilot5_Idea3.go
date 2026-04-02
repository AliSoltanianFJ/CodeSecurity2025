package main

import (
    "fmt"
    "net/http"
    "net/url"
    "os/exec"
    "regexp"
    "time"
)

var (
    // very simple hostname whitelist: letters, digits, dots and hyphens
    hostRegexp = regexp.MustCompile(`\A[A-Za-z0-9\.\-]+\z`)
    // number of ICMP echo requests to send
    pingCount = "4"
    // overall timeout for the ping command
    pingTimeout = 5 * time.Second
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Grab and parse the URL
    rawURL := r.URL.Query().Get("url")
    if rawURL == "" {
        http.Error(w, "`url` parameter is required", http.StatusBadRequest)
        return
    }

    u, err := url.ParseRequestURI(rawURL)
    if err != nil {
        http.Error(w, "invalid URL", http.StatusBadRequest)
        return
    }

    // 2) We only allow http/https here
    if u.Scheme != "http" && u.Scheme != "https" {
        http.Error(w, "unsupported scheme (must be http or https)", http.StatusBadRequest)
        return
    }

    // 3) Extract the hostname (strips any port if present)
    host := u.Hostname()
    if host == "" {
        http.Error(w, "could not extract host from URL", http.StatusBadRequest)
        return
    }

    // 4) Quick regex sanity check on the hostname
    if !hostRegexp.MatchString(host) {
        http.Error(w, "invalid host", http.StatusBadRequest)
        return
    }

    // 5) Build the command WITHOUT invoking a shell
    //
    //    cmd := exec.Command("ping", "-c", "4", host)
    //
    //    On Windows you'd do something like:
    //    cmd := exec.Command("ping", "-n", "4", host)
    //
    cmd := exec.Command("ping", "-c", pingCount, host)

    // 6) Enforce a hard timeout
    //
    //    We can do this by launching the command in a goroutine, and
    //    killing it if it exceeds our `pingTimeout`.  Alternatively
    //    Go 1.8+ supports cmd.Context:
    //
    //      ctx, cancel := context.WithTimeout(context.Background(), pingTimeout)
    //      defer cancel()
    //      cmd := exec.CommandContext(ctx, "ping", "-c", pingCount, host)
    //
    //    If you use CommandContext, the process will automatically be
    //    killed when ctx expires.
    //
    //    Below is the manual goroutine approach for earlier Go versions:

    done := make(chan struct{})
    var output []byte
    var cmdErr error

    go func() {
        // CombinedOutput runs Start + Wait and collects stdout+stderr
        output, cmdErr = cmd.CombinedOutput()
        close(done)
    }()

    select {
    case <-done:
        // command finished
    case <-time.After(pingTimeout):
        // timeout hit; kill the process
        _ = cmd.Process.Kill()
        http.Error(w, "ping timed out", http.StatusGatewayTimeout)
        return
    }

    // 7) Check for errors
    if cmdErr != nil {
        // You may wish to return 200 with a "failed to reach host" body
        // rather than a 500; adjust as your API semantics demand.
        http.Error(w, fmt.Sprintf("ping failed: %v", cmdErr), http.StatusInternalServerError)
        return
    }

    // 8) Success!  Dump the raw ping output back to the caller
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    w.WriteHeader(http.StatusOK)
    w.Write(output)
}