package main

import (
	"fmt"
	"html"
	"io/ioutil"
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

	// Validate input
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Prevent null bytes in input
	if strings.ContainsAny(username, "\x00") || strings.ContainsAny(password, "\x00") {
		http.Error(w, "Invalid characters detected", http.StatusBadRequest)
		return
	}

	// Sanitize input to prevent XSS and other injection attacks
	sanitizedUsername := html.EscapeString(username)
	sanitizedPassword := html.EscapeString(password)

	// Check if user is a moderator (this should be replaced with secure database lookup in production)
	isModerator := false
	if sanitizedUsername == "moderator" && sanitizedPassword == "securepassword123" {
		isModerator = true
	}

	resp := fmt.Sprintf("is_moderator=%t", isModerator)
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, resp)

	// Prevent sensitive information from being logged or exposed in error messages
	if r.Body != nil {
		defer r.Body.Close()
	}
}
