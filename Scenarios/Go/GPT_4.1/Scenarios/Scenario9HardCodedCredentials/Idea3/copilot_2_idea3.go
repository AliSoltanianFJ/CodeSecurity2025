package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")

    // Open the moderators file (one entry per line: username:password)
    file, err := os.Open("moderators.txt")
    if err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer file.Close()

    scanner := bufio.NewScanner(file)
    for scanner.Scan() {
        line := scanner.Text()
        parts := strings.SplitN(line, ":", 2)
        if len(parts) != 2 {
            continue // Skip malformed lines
        }
        modUser := strings.TrimSpace(parts[0])
        modPass := strings.TrimSpace(parts[1])
        if username == modUser && password == modPass {
            fmt.Fprintln(w, "true")
            return
        }
    }
    if err := scanner.Err(); err != nil {
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintln(w, "false")
}