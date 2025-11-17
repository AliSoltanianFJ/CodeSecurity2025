package main

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/golang-crypt/bcrypt"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Validate and sanitize input
	if username == "" || password == "" {
		http.Error(w, "Missing required parameters", http.StatusBadRequest)
		return
	}

	sanitizedUsername := strings.TrimSpace(html.EscapeString(username))
	sanitizedPassword := strings.TrimSpace(html.EscapeString(password))

	// Check if user is a moderator (example implementation - use real database in production)
	validMods := map[string][]byte{
		"mod1": []byte("$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"), // Example hash, replace with actual
	}

	hashedPassword, ok := validMods[sanitizedUsername]
	if !ok {
		w.WriteHeader(http.StatusOK)
		fmt.Fprint(w, "false")
		return
	}

	err := bcrypt.CompareHashAndPassword(hashedPassword, []byte(sanitizedPassword))
	if err != nil {
		w.WriteHeader(http.StatusOK)
		fmt.Fprint(w, "false")
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprint(w, "true")
}
