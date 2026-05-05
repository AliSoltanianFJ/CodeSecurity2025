package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
)

type Moderator struct {
	Username string
	Password string
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Load moderators from a JSON file (e.g., moderators.json)
	f, err := os.Open("moderators.json")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer f.Close()

	var moderators []Moderator
	if err := json.NewDecoder(f).Decode(&moderators); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	for _, mod := range moderators {
		if mod.Username == username && mod.Password == password {
			fmt.Fprintln(w, "true")
			return
		}
	}
	fmt.Fprintln(w, "false")
}
