package main

import (
	"crypto/hmac"
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/sessions"
)

var sessionStore = sessions.NewCookieStore([]byte("your-secure-key-here"))

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
		session, err := sessionStore.Get(r, "session-name")
		if err != nil {
			http.Error(w, "Invalid session", http.StatusBadRequest)
			return
		}
		if session.Values["username"] == nil {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, `<html>Hello! <a href='/login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	fmt.Fprint(w, `
		<html><form action="/login_post" method="post">
			<p><input type="text" name="username" placeholder="Username" required></p>
			<p><input type="password" name="password" placeholder="Password" required></p>
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

	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// In a real application, use proper user storage and hashing
	if username != "admin" || !isCorrectPassword(password) {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	session, err := sessionStore.Get(r, "session-name")
	if err != nil {
		http.Error(w, "Session error", http.StatusInternalServerError)
		return
	}
	session.Values["username"] = username
	session.Save(r.Context(), w)

	http.Redirect(w, r, "/settings", http.StatusFound)
}

func isCorrectPassword(password string) bool {
	// Use proper password hashing in production (e.g., bcrypt)
	return password == "password"
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	session, err := sessionStore.Get(r, "session-name")
	if err != nil {
		http.Error(w, "Invalid session", http.StatusBadRequest)
		return
	}
	fmt.Fprintf(w, "<html>Welcome to your settings, %s!</html>", session.Values["username"])
}

func generateSessionToken() (string, error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", fmt.Errorf("failed to generate token: %v", err)
	}
	return base64.URLEncoding.EncodeToString(b), nil
}

// Add middleware for secure headers
func secureHeaders(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Frame-Options", "DENY")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("X-XSS-Protection", "1; mode=block")
		w.Header().Set("Strict-Transport-Security", "max-age=31536000")
		w.Header().Set("Content-Security-Policy", "default-src 'self'")
		next(w, r)
	}
}

func init() {
	http.HandleFunc("/", secureHeaders(indexHandler))
	http.HandleFunc("/login", secureHeaders(loginHandler))
	http.HandleFunc("/login_post", secureHeaders(doLoginHandler))
	http.HandleFunc("/settings", secureHeaders(ensureLoggedIn(settingsHandler)))
}

// Add rate limiting middleware
func rateLimiter(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// Implement rate limiting logic here
		next(w, r)
	}
}

// Add CSRF protection middleware
func csrfProtection(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// Implement CSRF token validation here
		next(w, r)
	}
}
