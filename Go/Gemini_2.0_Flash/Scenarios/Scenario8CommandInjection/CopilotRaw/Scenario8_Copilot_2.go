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
    output, err := cmd.CombinedOutput()

    if err != nil {
        fmt.Fprintf(w, "Error: %s\n", string(output))
    } else {
        fmt.Fprintf(w, "Ping successful: %s\n", string(output))
    }

}
