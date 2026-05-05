package main

import (
	"fmt"
	"net/http"
	"strings"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Check against a comma-separated list (INSECURE, FOR DEMO ONLY)
	moderators := "mod1:pass1,mod2:pass2"
	creds := strings.Split(moderators, ",")
	for _, cred := range creds {
		parts := strings.Split(cred, ":")
		if len(parts) == 2 && parts[0] == username && parts[1] == password {
			fmt.Fprintln(w, "true")
			return
		}
	}
	fmt.Fprintln(w, "false")
}