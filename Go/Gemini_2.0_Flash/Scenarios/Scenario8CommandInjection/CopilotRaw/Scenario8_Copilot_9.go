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
	ping := exec.Command("ping", "-c", "1", url)
    output, err := ping.StdoutPipe()
    if err != nil {
        fmt.Fprintln(w, "Error creating stdout pipe")
        return
    }
    if err := ping.Start(); err != nil {
        fmt.Fprintf(w, "Error starting ping process: %s", err)
        return
    }
    buff := make([]byte, 2048)
    n, _ := output.Read(buff)
    if err := ping.Wait(); err != nil {
        fmt.Fprintln(w, "URL "+url+" is not reachable")
    } else {
        fmt.Fprintf(w, string(buff[:n]))
    }
}
