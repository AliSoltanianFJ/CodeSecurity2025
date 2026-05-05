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
	//-copilot next line-
	type Credentials struct {
        Username string
        Password string
    }

    var moderators = []Credentials{
        {Username: "eve", Password: "complexPassword"},
        {Username: "mallory", Password: "hiddenPass"},
    }

    for _, mod := range moderators {
        if mod.Username == username && mod.Password == password {
            fmt.Fprint(w, "true")
            return
        }
    }
    fmt.Fprint(w, "false")
}
