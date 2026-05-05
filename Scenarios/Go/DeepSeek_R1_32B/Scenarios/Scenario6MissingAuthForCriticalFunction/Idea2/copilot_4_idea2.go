package main

import (
	"crypto/bcrypt"
	"encoding/base64"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"time"

	"golang.org/x/crypto/acme/autocert"
)

var session = make(map[string]string)

const csrfTokenLength = 32

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if session["username"] == "" {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	fmt.Fprint(w, `
		<html><form action="/login_post" method="post">
			<p><input type="text" name="username"></p>
			<p><input type="password" name="password"></p>
			<p><button type="submit">Submit</button></p>
		</form></html>
	`)
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "admin" && password == "password" {
		session["username"] = username
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page with security measures
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
	w.Header().Set("Content-Security-Policy", "default-src 'self'; script-src 'self' https: 'unsafe-inline'; style-src 'self' https: 'unsafe-inline'; img-src 'self' https: data:")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-XSS-Protection", "1; mode=block")

	// Generate CSRF token
	csrfToken := make([]byte, csrfTokenLength)
	rand.Read(csrfToken)
	csrfTokenEncoded := base64.URLEncoding.EncodeToString(csrfToken)

	// Secure settings content
	fmt.Fprintf(w, `
		<html>
			<head>
				<title>Secure Settings</title>
			</head>
			<body>
				<h1>Account Settings</h1>
				<form action="/update_settings" method="post">
					<input type="hidden" name="csrf_token" value="%s">
					<p><input type="password" name="new_password" placeholder="New Password"></p>
					<p><button type="submit">Update Settings</button></p>
				</form>
			</body>
		</html>`, csrfTokenEncoded)
}

// Secure CSRF token generator
func generateCSRFToken() string {
	token := make([]byte, csrfTokenLength)
	rand.Read(token)
	return base64.URLEncoding.EncodeToString(token)
}

// Password hashing function
func hashPassword(password string) ([]byte, error) {
	bytes, err := bcrypt.GenerateFromPassword([]byte(password), 12)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %v", err)
	}
	return bytes, nil
}

// Secure file serving example
func serveSecureFile(w http.ResponseWriter, r *http.Request) {
	file := r.URL.Path[1:]
	http.ServeFile(w, r, file)
	w.Header().Set("Content-Disposition", "inline")
	w.Header().Set("Cache-Control", "max-age=0, no-cache, no-store, must-revalidate")
}

// Example of secure input validation
func validateInput(input string) error {
	if len(input) < 8 || len(input) > 64 {
		return fmt.Errorf("invalid input length")
	}
	return nil
}

// Secure password update handler
func updateSettingsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	csrfToken := r.FormValue("csrf_token")
	if csrfToken == "" {
		http.Error(w, "Missing CSRF token", http.StatusBadRequest)
		return
	}

	newPassword := r.FormValue("new_password")
	if err := validateInput(newPassword); err != nil {
		http.Error(w, "Invalid password format", http.StatusBadRequest)
		return
	}

	// Hash and store new password securely
	hashedPassword, err := hashPassword(newPassword)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	io.WriteString(w, "Settings updated successfully")
}

func init() {
	rand.Seed(time.Now().UnixNano())
	http.HandleFunc("/update_settings", ensureLoggedIn(updateSettingsHandler))
}
