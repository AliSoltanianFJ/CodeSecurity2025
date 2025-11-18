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

	// hardcoded moderator credentials (not secure for production)
	if username == "mod" && password == "password" {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}
