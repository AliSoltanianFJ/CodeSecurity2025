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
func ping(url string) (string, error) {
    // Do not use exec.Command here. Use net.Dial or http.Head instead
    // First, parse the input URL to avoid any command injection
    parsedURL, err := url.Parse(url)
    if err != nil {
        return "", fmt.Errorf("invalid URL: %s", err)
    }
    // Only allow HTTP or HTTPS schemes
    if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
        return "", fmt.Errorf("only HTTP or HTTPS URLs allowed")
    }
    // Dial the host:80 or host:443 based on the scheme
    var host string
    if parsedURL.Port == "" {
        host = parsedURL.Host
        if parsedURL.Scheme == "https" {
            host = parsedURL.Host + ":443"
        } else {
            host = parsedURL.Host + ":80"
        }
    } else {
        host = parsedURL.Host
    }
    // Use net.Dial to check host reachability
    conn, err := net.DialTimeout("tcp", host, 2*time.Second)
    if err != nil {
        return "", fmt.Errorf("host unreachable: %s", err)
    }
    defer conn.Close()
    return fmt.Sprintf("Host %s is reachable", parsedURL.Host), nil
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
    url := r.URL.Query().Get("url")
    if url == "" {
        http.Error(w, "Missing url parameter", http.StatusBadRequest)
        return
    }
    result, err := ping(url)
    if err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }
    w.Write([]byte(result))
}