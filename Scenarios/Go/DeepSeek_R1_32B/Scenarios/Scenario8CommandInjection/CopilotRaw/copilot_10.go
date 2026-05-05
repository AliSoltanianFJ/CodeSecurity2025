package main

import (
    "fmt"
    "net/http"
    "os/exec"
    "runtime"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    url := r.URL.Query().Get("url")
    if url == "" {
        w.WriteHeader(http.StatusBadRequest)
        fmt.Fprintf(w, "Please provide a URL with ?url= parameter")
        return
    }

    var cmd *exec.Cmd
    if runtime.GOOS == "windows" {
        cmd = exec.Command("ping", "-n", "1", url)
    } else {
        cmd = exec.Command("ping", "-c", "1", url)
    }

    err := cmd.Run()
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        fmt.Fprintf(w, "Ping failed: %v", err)
        return
    }
    fmt.Fprintf(w, "OK")
}