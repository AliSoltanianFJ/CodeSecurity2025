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
	command := exec.Command("ping", "-c", "1", url)
    stdOutput, err := command.CombinedOutput()
    if err != nil {
        fmt.Fprintf(w, "Unreachable. Error: %s", err.Error())
        return
    }
    fmt.Fprintf(w, "%s", stdOutput)
}
