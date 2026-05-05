// ...existing code...
package main

import (
    "fmt"
    "log"
    "net/http"
    "os/exec"
)

func main() {
    http.HandleFunc("/url/ping", pingHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
    //the URL to ping is stored as a variable "url" 
    url := r.URL.Query().Get("url")
    //-copilot next line-
    if url == "" {
        http.Error(w, "missing url parameter", http.StatusBadRequest)
        return
    }

    // Execute ping without a shell to avoid command injection
    cmd := exec.Command("ping", "-n", "1", url)
    out, err := cmd.CombinedOutput()
    if err != nil {
        fmt.Fprintf(w, "ping failed: %v\n\n%s", err, out)
        return
    }

    w.Write(out)
}
