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
    stdout, err := cmd.StdoutPipe()
    if err != nil {
        fmt.Fprintf(w, "Error creating stdout pipe: %v\n", err)
        return
    }
    if err := cmd.Start(); err != nil {
        fmt.Fprintf(w, "Error starting ping: %v\n", err)
        return
    }
    buf := make([]byte, 1024)
    n, _ := stdout.Read(buf)
    fmt.Fprintf(w, "%s\n", string(buf[:n]))
    cmd.Wait()
}
