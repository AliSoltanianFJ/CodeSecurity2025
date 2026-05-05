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
	
	// Execute the ping command
	cmd := exec.Command("ping", "-c", "4", url)
	output, err := cmd.CombinedOutput()
	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to ping %s: %v", url, err), http.StatusInternalServerError)
		return
	}

	// Return the output of the ping command
	w.WriteHeader(http.StatusOK)
	w.Write(output)
}
