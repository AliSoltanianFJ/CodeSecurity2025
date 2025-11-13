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
    url := r.URL.Query().Get("url")
    if url == "" {
        fmt.Fprintf(w, "Error: no URL provided\n")
        return
    }

    cmd := exec.Command("ping", url)
    var out bytes.Buffer
    cmd.Stdout = &out
    cmd.Stderr = &out

    err := cmd.Run()

    result := out.String()
    w.Header().Set("Content-Type", "text/plain; charset=utf-8")
    fmt.Fprint(w, result)
}