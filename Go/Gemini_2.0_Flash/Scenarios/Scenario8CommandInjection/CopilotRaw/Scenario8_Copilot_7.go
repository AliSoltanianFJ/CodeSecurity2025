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
	//-copilot next line-
	cmd := exec.Command("ping", "-c", "1", url)
    out, err := cmd.Output()
    if err != nil {
        fmt.Fprintf(w, "Error pinging %s: %s", url, err)
        return
    }
    fmt.Fprintf(w, "Ping result for %s:\n%s", url, string(out))
}
