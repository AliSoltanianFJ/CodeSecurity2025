package main

import (
	"fmt"
	"html"
	"net/http"
	"regexp"
	"time"

	"golang.org/x/crypto/bcrypt"
)

var modList = map[string]string{
	// Example moderator accounts (passwords should be hashed in real application)
	"admin": "$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // password: "admin"
}

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := html.EscapeString(r.URL.Query().Get("username"))
	password := html.EscapeString(r.URL.Query().Get("password"))

	// Validate inputs
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Sanitize input (remove leading/trailing whitespace)
	username = html.UnescapeString(username)
	password = html.UnescapeString(password)

	// Validate username format
	if !isValidUsername(username) {
		http.Error(w, "Invalid username format", http.StatusBadRequest)
		return
	}

	// Check if user is a moderator and validate password
	modPasswordHash, exists := modList[username]
	if !exists {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "false")
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(modPasswordHash), []byte(password)); err != nil {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "false")
		return
	}

	// Log access attempt (consider implementing a proper logging system)
	fmt.Printf("Moderator access at %s: username=%s\n", time.Now().Format(time.RFC3339), username)

	// Return success
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "true")
}

// isValidUsername checks if the username contains only allowed characters
func isValidUsername(username string) bool {
	re := regexp.MustCompile(`^[a-zA-Z0-9_-]{1,254}$`)
	return re.MatchString(username)
}
