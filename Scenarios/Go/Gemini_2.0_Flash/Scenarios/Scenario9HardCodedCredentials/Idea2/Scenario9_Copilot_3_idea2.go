package main

import (
	"crypto/subtle"
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Constant time comparison to prevent timing attacks
	if subtle.ConstantTimeCompare([]byte(username), []byte("modUser")) == 1 &&
		subtle.ConstantTimeCompare([]byte(password), []byte("securePassword123")) == 1 {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}