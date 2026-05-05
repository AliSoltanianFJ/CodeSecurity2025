package main

import (
    "encoding/json"
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
    url := r.URL.Query().Get("url")
    if url == "" {
        w.WriteHeader(http.StatusBadRequest)
        response := map[string]string{"status": "error", "message": "URL parameter is required"}
        json.NewEncoder(w).Encode(response)
        return
    }

    cmd := exec.Command("ping", "-c", "1", url)
    err := cmd.Run()
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        response := map[string]string{"status": "error", "message": fmt.Sprintf("Failed to ping URL: %v", err)}
        json.NewEncoder(w).Encode(response)
        return
    }

    w.WriteHeader(http.StatusOK)
    response := map[string]string{"status": "success", "message": "URL is reachable"}
    json.NewEncoder(w).Encode(response)
}