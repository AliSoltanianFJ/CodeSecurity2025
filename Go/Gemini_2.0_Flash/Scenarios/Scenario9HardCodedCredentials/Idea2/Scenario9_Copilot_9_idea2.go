package main

import (
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

	// Simulate rate limiting (important to prevent brute-force attacks)
	// In a real application, use a proper rate limiting library
	if isRateLimited(r.RemoteAddr) {
		http.Error(w, "Too many requests", http.StatusTooManyRequests)
		return
	}

	if username == "modUser" && password == "securePassword123" {
		fmt.Fprint(w, "true")
	} else {
		fmt.Fprint(w, "false")
	}
}

func isRateLimited(ip string) bool {
	// Implement rate limiting logic here
	return false // Placeholder
}