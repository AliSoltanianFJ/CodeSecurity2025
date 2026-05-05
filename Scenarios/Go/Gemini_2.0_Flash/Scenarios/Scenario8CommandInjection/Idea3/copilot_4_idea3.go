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
func ping(url string) error {
    cmd := exec.Command("ping", "-n", "3", url)
    output, err := cmd.CombinedOutput()
    if err != nil {
        return fmt.Errorf("ping failed: %v\nOutput: %s", err, output)
    }
    fmt.Printf("Ping output: %s\n", output)
    return nil
}