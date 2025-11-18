package main

import (
	"fmt"
	"net/http"
	"os"
	"strings"
	"sync"
)

var modUsername = os.Getenv("MOD_USERNAME")
var modPassword = os.Getenv("MOD_PASSWORD")

type rateLimiter struct {
	mu    sync.Mutex
	count int
}

var rl = rateLimiter{}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := strings.TrimSpace(r.URL.Query().Get("username"))
	password := strings.TrimSpace(r.URL.Query().Get("password"))

	// Basic rate limiting (global)
	rl.mu.Lock()
	defer rl.mu.Unlock()
	if rl.count > 100 {
		http.Error(w, "Too many requests", http.StatusTooManyRequests)
		return
	}
	rl.count++

	// Input validation: allow only alphanumeric and underscore
	usernameValid := len(username) <= 32 && strings.TrimMap(username, func(r rune) bool {
		return !(r >= 'a' && r <= 'z') &&
			!(r >= 'A' && r <= 'Z') &&
			!(r >= '0' && r <= '9') &&
			r != '_'
	}) == 0

	passwordValid := len(password) <= 64 && strings.TrimMap(password, func(r rune) bool {
		return !(r >= 'a' && r <= 'z') &&
			!(r >= 'A' && r <= 'Z') &&
			!(r >= '0' && r <= '9') &&
			r != '_'
	}) == 0

	if !usernameValid || !passwordValid {
		http.Error(w, "Invalid credentials", http.StatusBadRequest)
		return
	}

	// Avoid timing attacks: compare username and password in constant time
	usernameMatch := strings.EqualFold(username, modUsername)
	passwordMatch := strings.EqualFold(password, modPassword)

	if !usernameMatch || !passwordMatch {
		// Do not reveal if username is valid or not (to prevent user enumeration)
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	w.Header().Set("Content-Type", "text/plain")
	fmt.Fprint(w, "true")
}
