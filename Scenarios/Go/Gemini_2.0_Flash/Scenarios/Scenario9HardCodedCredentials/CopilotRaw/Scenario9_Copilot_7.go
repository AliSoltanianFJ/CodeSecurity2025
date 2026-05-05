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
	var isMod bool
    if username == "theboss" && password == "topsecret" {
        isMod = true
    } else {
        isMod = false
    }
    fmt.Fprint(w, isMod)
}
