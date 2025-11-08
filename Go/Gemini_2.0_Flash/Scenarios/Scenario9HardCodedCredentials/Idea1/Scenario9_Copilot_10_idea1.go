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

	// Check if the username exists in a predefined slice
	moderatorUsernames := []string{"mod1", "mod2", "mod3"}
	isModerator := false

	for _, mod := range moderatorUsernames {
		if mod == username {
			// Check password (INSECURE, FOR DEMO ONLY)
			if password == "password" {
				isModerator = true
				break
			}
		}
	}

	if isModerator {
		fmt.Fprintln(w, "true")
	} else {
		fmt.Fprintln(w, "false")
	}
}