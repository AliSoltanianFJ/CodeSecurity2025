package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os/exec"
	"time"
)

func main() {
	http.HandleFunc("/url/ping", pingHandler)
	http.ListenAndServe(":8080", nil)
}

/* ping a URL to see if it is alive using "ping" system command */
func pingHandler(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Query().Get("url")

	if url == "" {
		http.Error(w, "URL is required", http.StatusBadRequest)
		return
	}

	if len(url) > 2048 {
		http.Error(w, "Invalid URL format or length", http.StatusBadRequest)
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), time.Duration(5)*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, "ping", "-c", "4", url)

	stdout, err := cmd.Output()
	if err != nil {
		http.Error(w, "Unable to execute ping command", http.StatusInternalServerError)
		return
	}

	response := map[string]string{
		"output": string(stdout),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
