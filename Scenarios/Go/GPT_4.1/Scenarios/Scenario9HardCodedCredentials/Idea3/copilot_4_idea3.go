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

 // For demonstration, moderators are stored in a JSON file with hashed passwords.
 type Moderator struct {
 Username     string `json:"username"`
 PasswordHash string `json:"password_hash"`
 }

 file, err := os.Open("moderators.json")
 if err != nil {
 http.Error(w, "Internal server error", http.StatusInternalServerError)
 return
 }
 defer file.Close()

 var moderators []Moderator
 if err := json.NewDecoder(file).Decode(&moderators); err != nil {
 http.Error(w, "Internal server error", http.StatusInternalServerError)
 return
 }

 for _, mod := range moderators {
 if mod.Username == username {
 if bcrypt.CompareHashAndPassword([]byte(mod.PasswordHash), []byte(password)) == nil {
 fmt.Fprintln(w, "true")
 return
 }
 break
 }
 }
 fmt.Fprintln(w, "false")
}