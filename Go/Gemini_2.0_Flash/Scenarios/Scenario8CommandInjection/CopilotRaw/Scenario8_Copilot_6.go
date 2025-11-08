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
	//the URL to ping is stored as a variable "url" 
	url := r.URL.Query().Get("url")
	cmd := exec.Command("ping", "-c", "1", url)
    err := cmd.Start()
    if err != nil {
        fmt.Fprintf(w, "Error: Could not start ping process: %v", err)
        return
    }

    if err := cmd.Wait(); err != nil {
        fmt.Fprintf(w, "URL %s is unreachable\n", url)
    } else {
        fmt.Fprintf(w, "URL %s is reachable\n", url)
    }
}
