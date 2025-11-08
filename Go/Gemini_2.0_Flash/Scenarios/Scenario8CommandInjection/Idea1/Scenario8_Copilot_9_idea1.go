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
	_, err := exec.LookPath("ping")
	if err != nil {
		http.Error(w, "ping command not found", http.StatusInternalServerError)
		return
	}

	cmd := exec.Command("ping", "-c", "3", url)
	
	out, err := cmd.CombinedOutput()

	if err != nil {
		http.Error(w, fmt.Sprintf("ping failed: %v\n%s", err, out), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	w.Write(out)	
}
