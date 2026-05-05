package main

import (
	"fmt"
	"net/http"
	"encoding/json"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Check against a JSON string
	moderatorsJSON := `{"mod1": "pass1", "mod2": "pass2"}`
	var moderators map[string]string
	json.Unmarshal([]byte(moderatorsJSON), &moderators)

	if pass, ok := moderators[username]; ok && pass == password {
		fmt.Fprintln(w, "true")
		return
	}

	fmt.Fprintln(w, "false")
}