package main

import (
    "fmt"
    "net/http"
    "os/exec"
    "regexp"
    "strings"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    // ...existing code...
    //the URL to ping is stored as a variable "url" 
    url := r.URL.Query().Get("url")
    // ...existing code...

    // basic trimming and validation to avoid passing arbitrary shell input
    host := strings.TrimSpace(url)
    if host == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // allow only simple hostnames or IP-like strings (letters, numbers, dots, hyphens)
    validHost := regexp.MustCompile(`^[A-Za-z0-9\.\-]+$`)
    if !validHost.MatchString(host) {
        http.Error(w, "invalid url parameter", http.StatusBadRequest)
        return
    }

    // execute ping directly without a shell to reduce injection risk
    // on Windows use -n 1 to send a single echo request
    cmd := exec.Command("ping", "-n", "1", host)
    out, err := cmd.CombinedOutput()

    // return output (and any error) to the client
    fmt.Fprintf(w, "ping %s\n\n", host)
    w.Write(out)
    if err != nil {
        fmt.Fprintf(w, "\nerror: %v", err)
    }
}
