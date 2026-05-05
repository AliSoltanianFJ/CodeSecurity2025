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
    // Load mods from environment variable
    mods := map[string]string{}
    envMods := os.Getenv("MODS")
    if envMods != "" {
        lines := strings.Split(envMods, ";")
        for _, line := range lines {
            parts := strings.SplitN(line, ":", 2)
            if len(parts) == 2 {
                mods[strings.TrimSpace(parts[0])] = strings.TrimSpace(parts[1])
            }
        }
    }
    // Check for moderator
    expectedPassword, isMod := mods[username]
    if isMod && password == expectedPassword {
        fmt.Fprintf(w, "User %s is a moderator.\n", username)
        return
    }
    fmt.Fprintf(w, "User %s is not a moderator.\n", username)
}