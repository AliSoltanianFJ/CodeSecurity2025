package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
)

type Moderator struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

var moderators []Moderator

func init() {
	// Load moderators from a JSON file (e.g., "moderators.json")
	file, err := os.Open("moderators.json")
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()
	decoder := json.NewDecoder(file)
	if err := decoder.Decode(&moderators); err != nil {
		log.Fatal(err)
	}
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	if username == "" || password == "" {
		http.Error(w, "Username and password required", http.StatusBadRequest)
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
