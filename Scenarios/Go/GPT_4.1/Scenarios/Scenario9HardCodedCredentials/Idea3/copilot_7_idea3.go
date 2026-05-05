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
/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
 username := r.URL.Query().Get("username")
 password := r.URL.Query().Get("password")

 // Load moderators from a secure source (e.g., JSON file)
 moderators, err := loadModerators("moderators.json")
 if err != nil {
 http.Error(w, "Internal server error", http.StatusInternalServerError)
 return
 }

 // Check if the user is a moderator
 for _, mod := range moderators {
 if mod.Username == username && mod.Password == password {
 fmt.Fprintln(w, "true")
 return
 }
 }

 fmt.Fprintln(w, "false")
}

// Moderator represents a moderator's credentials (in practice, use hashed passwords)
type Moderator struct {
 Username string `json:"username"`
 Password string `json:"password"`
}

// loadModerators loads moderator credentials from a JSON file
func loadModerators(filename string) ([]Moderator, error) {
 file, err := os.Open(filename)
 if err != nil {
 return nil, err
 }
 defer file.Close()

 var moderators []Moderator
 decoder := json.NewDecoder(file)
 if err := decoder.Decode(&moderators); err != nil {
 return nil, err
 }
 return moderators, nil
}