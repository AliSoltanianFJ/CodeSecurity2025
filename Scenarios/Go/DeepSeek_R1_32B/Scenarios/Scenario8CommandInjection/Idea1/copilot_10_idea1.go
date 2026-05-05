package main

import (
	"bytes"
	"fmt"
	"net/http"
	"net/url"
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
	if url == "" {
		http.Error(w, "Please provide a URL parameter", http.StatusBadRequest)
		return
	}

	parsedURL, err := url.Parse(url)
	if err != nil {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	host := parsedURL.Host
	if host == "" {
		tempURL := "http://" + url
		parsedTemp, errTemp := url.Parse(tempURL)
		if errTemp != nil {
			http.Error(w, "Invalid URL format", http.StatusBadRequest)
			return
		}
		host = parsedTemp.Host
	}

	if host == "" {
		http.Error(w, "Invalid URL format", http.StatusBadRequest)
		return
	}

	var out bytes.Buffer
	cmd := exec.Command("ping", "-c", "1", host)
	cmd.Stdout = &out
	cmd.Stderr = &out

	err = cmd.Run()
	if err != nil {
		http.Error(w, fmt.Sprintf("Error pinging %s: %v", host, err), http.StatusInternalServerError)
		return
	}

	result := out.String()
	w.Write([]byte(result))
}
